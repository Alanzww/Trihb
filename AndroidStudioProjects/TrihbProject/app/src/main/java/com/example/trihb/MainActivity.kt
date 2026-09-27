package com.example.trihb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.trihb.databinding.ActivityMainBinding
import android.content.Intent
import android.view.View
import android.view.animation.Animation
import android.widget.Toast
import android.view.animation.AnimationUtils


class MainActivity : ComponentActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // 1. Infla a interface via View Binding
         val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Trata insets de barra de status e navegação
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 3. Navegação para tela de login/registro via botão
        binding.bttInit.setOnClickListener {
            Toast.makeText(this, "Bem-vindo a área de Login/Registro!", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            finish()
        }
    }
}
