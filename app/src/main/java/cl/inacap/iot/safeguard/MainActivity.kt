package cl.inacap.iot.safeguard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cl.inacap.iot.safeguard.databinding.ActivityMainBinding
import cl.inacap.iot.safeguard.security.SecurityManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        securityManager = SecurityManager.getInstance(this)

        if (!securityManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        setupUserSession()
        setupListeners()
    }

    private fun setupUserSession() {
        val username = securityManager.getUsername() ?: "Desconocido"
        val role = securityManager.getUserRole() ?: "OPERATOR"
        binding.tvUserSession.text = "Operador autenticado: $username | Rol: $role"
    }

    private fun setupListeners() {
        binding.btnOpenSimulator.setOnClickListener {
            startActivity(Intent(this, SimulatorActivity::class.java))
        }

        binding.cardSimulator.setOnClickListener {
            startActivity(Intent(this, SimulatorActivity::class.java))
        }

        binding.btnOpenDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        binding.cardDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            securityManager.clearSession()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
