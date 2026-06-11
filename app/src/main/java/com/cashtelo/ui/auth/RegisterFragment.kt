package com.cashtelo.ui.auth

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.cashtelo.R
import com.cashtelo.data.entity.User
import com.cashtelo.databinding.FragmentRegisterBinding
import com.cashtelo.viewmodel.UserViewModel
import com.cashtelo.viewmodel.UserViewModelFactory

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels {
        UserViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(500).start()

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etRegisterName.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString()
            val confirmPassword = binding.etRegisterConfirmPassword.text.toString()

            if (name.isEmpty()) {
                binding.etRegisterName.error = "Digite seu nome"
                return@setOnClickListener
            }

            if (password.isNotEmpty() && password != confirmPassword) {
                binding.etRegisterConfirmPassword.error = "As senhas não coincidem"
                return@setOnClickListener
            }

            val user = User(name = name, password = password)
            userViewModel.saveUser(user)
            Toast.makeText(requireContext(), "Conta criada! Bem-vindo(a), $name!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
