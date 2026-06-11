package com.cashtelo.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import java.io.File
import java.io.FileOutputStream
import com.cashtelo.data.entity.User
import com.cashtelo.databinding.FragmentProfileBinding
import com.cashtelo.viewmodel.UserViewModel
import com.cashtelo.viewmodel.UserViewModelFactory

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentUser: User? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processImageUri(it) }
    }

    private val viewModel: UserViewModel by viewModels {
        UserViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Entrada animada
        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(400).start()

        binding.containerAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        observeViewModel()

        binding.btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Sair")
                .setMessage("Deseja sair da sua conta?")
                .setPositiveButton("Sair") { _, _ ->
                    val navController = androidx.navigation.Navigation
                        .findNavController(requireActivity(), com.cashtelo.R.id.nav_host_fragment)
                    navController.navigate(
                        com.cashtelo.R.id.loginFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(com.cashtelo.R.id.nav_graph, true)
                            .build()
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.btnSaveProfile.setOnClickListener {
            binding.etUserName.error = null
            binding.etCurrentPassword.error = null

            val name = binding.etUserName.text.toString().trim()
            val currentPasswordInput = binding.etCurrentPassword.text.toString().trim()
            val newPassword = binding.etUserPassword.text.toString().trim()
            
            if (name.isEmpty()) {
                binding.etUserName.error = "Por favor, digite seu nome."
                return@setOnClickListener
            }

            // Se existe uma senha salva e o usuário quer trocá-la, exige a senha atual correta
            val savedPassword = currentUser?.password ?: ""
            if (savedPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                if (currentPasswordInput != savedPassword) {
                    binding.etCurrentPassword.error = "Senha atual incorreta."
                    return@setOnClickListener
                }
            }

            val finalPassword = if (newPassword.isNotEmpty()) newPassword else savedPassword

            val userToSave = currentUser?.copy(name = name, password = finalPassword)
                ?: User(name = name, password = finalPassword)
            
            currentUser = userToSave
            viewModel.saveUser(userToSave)
            
            hideKeyboard()
            Toast.makeText(requireContext(), "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
            
            // Limpa os campos de senha após salvar
            binding.etCurrentPassword.text?.clear()
            binding.etUserPassword.text?.clear()
            
            // Animação de feedback no botão
            it.animate()
                .scaleX(0.95f).scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
                .start()
        }
        setupPasswordStrengthWatcher()
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                currentUser = user
                
                // Avatar dinâmico
                val avatarPath = user.avatarUri
                if (!avatarPath.isNullOrEmpty() && File(avatarPath).exists()) {
                    val bitmap = BitmapFactory.decodeFile(avatarPath)
                    binding.ivProfileAvatar.setImageBitmap(bitmap)
                    binding.ivProfileAvatar.imageTintList = null
                    binding.ivProfileAvatar.setPadding(0, 0, 0, 0)
                    binding.ivProfileAvatar.visibility = View.VISIBLE
                    binding.tvAvatarInitials.visibility = View.GONE
                } else if (user.name.isNotEmpty()) {
                    val initial = user.name.first().uppercaseChar().toString()
                    binding.tvAvatarInitials.text = initial
                    binding.tvAvatarInitials.visibility = View.VISIBLE
                    binding.ivProfileAvatar.visibility = View.GONE
                }

                if (binding.etUserName.text.toString().isEmpty()) {
                    binding.etUserName.setText(user.name)
                }

                setupBiometricSwitch(user)
            }
        }
    }

    private fun setupPasswordStrengthWatcher() {
        binding.etUserPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                if (password.isEmpty()) {
                    binding.layoutPasswordStrength.visibility = View.GONE
                    binding.tvPasswordFeedback.visibility = View.GONE
                    return
                }

                binding.layoutPasswordStrength.visibility = View.VISIBLE
                binding.tvPasswordFeedback.visibility = View.VISIBLE

                val colorRed = android.graphics.Color.parseColor("#E53935")
                val colorYellow = android.graphics.Color.parseColor("#FFB300")
                val colorGreen = android.graphics.Color.parseColor("#43A047")
                val colorGray = android.graphics.Color.parseColor("#40FFFFFF")

                if (password.length < 4) {
                    binding.pwdStrength1.setBackgroundColor(colorRed)
                    binding.pwdStrength2.setBackgroundColor(colorGray)
                    binding.pwdStrength3.setBackgroundColor(colorGray)
                    binding.tvPasswordFeedback.text = "Senha Fraca"
                    binding.tvPasswordFeedback.setTextColor(colorRed)
                } else if (password.length in 4..6) {
                    binding.pwdStrength1.setBackgroundColor(colorYellow)
                    binding.pwdStrength2.setBackgroundColor(colorYellow)
                    binding.pwdStrength3.setBackgroundColor(colorGray)
                    binding.tvPasswordFeedback.text = "Senha Média"
                    binding.tvPasswordFeedback.setTextColor(colorYellow)
                } else {
                    binding.pwdStrength1.setBackgroundColor(colorGreen)
                    binding.pwdStrength2.setBackgroundColor(colorGreen)
                    binding.pwdStrength3.setBackgroundColor(colorGreen)
                    binding.tvPasswordFeedback.text = "Senha Forte"
                    binding.tvPasswordFeedback.setTextColor(colorGreen)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupBiometricSwitch(user: User) {
        binding.switchBiometrics.setOnCheckedChangeListener(null)
        binding.switchBiometrics.isChecked = user.useBiometrics
        
        binding.switchBiometrics.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showBiometricPrompt { success ->
                    if (success) {
                        viewModel.saveUser(user.copy(useBiometrics = true))
                        Toast.makeText(requireContext(), "Biometria ativada!", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.switchBiometrics.isChecked = false
                    }
                }
            } else {
                viewModel.saveUser(user.copy(useBiometrics = false))
            }
        }
    }
    
    private fun showBiometricPrompt(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Biometria: $errString", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(false)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticação Biométrica")
            .setSubtitle("Confirme sua identidade para habilitar no app")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun processImageUri(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Comprimir e redimensionar para máximo 256x256
                val scaledBitmap = scaleBitmap(bitmap, 256)
                val file = File(requireContext().filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                val out = FileOutputStream(file)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.flush()
                out.close()

                // Atualiza a View e Salva no DB
                binding.ivProfileAvatar.setImageBitmap(scaledBitmap)
                binding.ivProfileAvatar.imageTintList = null
                binding.ivProfileAvatar.setPadding(0, 0, 0, 0)
                binding.ivProfileAvatar.visibility = View.VISIBLE
                binding.tvAvatarInitials.visibility = View.GONE

                val updatedUser = currentUser?.copy(avatarUri = file.absolutePath) ?: User(avatarUri = file.absolutePath, name = "", password = "")
                currentUser = updatedUser
                viewModel.saveUser(updatedUser)
                Toast.makeText(requireContext(), "Foto atualizada!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao processar imagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio: Float = width.toFloat() / height.toFloat()
        if (ratio > 1) {
            width = maxSize
            height = (width / ratio).toInt()
        } else {
            height = maxSize
            width = (height * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
