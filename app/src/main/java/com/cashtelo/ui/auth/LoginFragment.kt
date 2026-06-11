package com.cashtelo.ui.auth

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.cashtelo.R
import com.cashtelo.databinding.FragmentLoginBinding
import com.cashtelo.viewmodel.UserViewModel
import com.cashtelo.viewmodel.UserViewModelFactory

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels {
        UserViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(500).start()

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                // Nenhum usuário cadastrado: vai direto para cadastro
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                return@observe
            }

            // Mostra opção biométrica se habilitada
            if (user.useBiometrics) {
                binding.btnBiometric.visibility = View.VISIBLE
                binding.btnBiometric.setOnClickListener {
                    showBiometricPrompt {
                        navigateToHome()
                    }
                }
            } else {
                binding.btnBiometric.visibility = View.GONE
            }
        }

        binding.btnLogin.setOnClickListener {
            val password = binding.etLoginPassword.text.toString()
            val user = userViewModel.user.value

            if (user == null) {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                return@setOnClickListener
            }

            if (user.password.isEmpty() || password == user.password) {
                navigateToHome()
            } else {
                binding.etLoginPassword.error = "Senha incorreta"
                Toast.makeText(requireContext(), "Senha incorreta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Erro: $errString", Toast.LENGTH_SHORT).show()
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Autenticação falhou", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Cashtelo")
            .setSubtitle("Use sua biometria para entrar")
            .setNegativeButtonText("Usar senha")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
