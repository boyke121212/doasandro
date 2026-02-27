package com.toelve.doas.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityProfileBinding
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.soasa.CryptoAES
import org.json.JSONObject

class ProfileActivity : Boyke() {

    private lateinit var binding: ActivityProfileBinding
    private val subditList = arrayOf("Subdit 1", "Subdit 2", "Subdit 3", "Subdit 4", "Subdit 5", "Staff Pimpinan")
    var subdit: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup Spinner Subdit
        val adapterSubdit = ArrayAdapter(this, android.R.layout.simple_spinner_item, subditList)
        adapterSubdit.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spSubdit.adapter = adapterSubdit

        // Listener Spinner Subdit
        binding.spSubdit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                subdit = subditList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadUserData()

        binding.btnSimpanProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnUbahPassword.setOnClickListener {
            changePassword()
        }
    }

    private fun loadUserData() {
        AuthManager(this, "api/getme").checkAuth(
            onLoading = { loading ->
                if (loading) showLoading()
                else hideLoading()
            },
            onSuccess = { json ->
                try {
                    val aesKey = json.getString("aes_key")
                    val data = json.getJSONObject("data")
                    
                    binding.apply {
                        etUsername.setText(decryptField(data, "username", aesKey))
                        etNIP.setText(decryptField(data, "nip", aesKey))
                        
                        // Mapping Role
                        val roleId = decryptField(data, "roleId", aesKey)
                        val roleText = when(roleId) {
                            "2" -> "Admin Utama"
                            "3" -> "Admin User"
                            "4" -> "Admin Berita"
                            "5" -> "Admin Anggaran"
                            "6" -> "Admin kantor"
                            "7" -> "Admin laporan"
                            "8" -> "User"
                            else -> roleId
                        }
                        etRole.setText(roleText)
                        
                        etNama.setText(decryptField(data, "nama", aesKey))
                        etPangkat.setText(decryptField(data, "pangkat", aesKey))
                        etJabatan.setText(decryptField(data, "jabatan", aesKey))
                        
                        // Set Spinner Subdit
                        val subditDb = decryptField(data, "subdit", aesKey)
                        val index = subditList.indexOf(subditDb)
                        if (index != -1) {
                            spSubdit.setSelection(index)
                            subdit = subditDb
                        }

                        // Lock field agar tidak bisa di edit
                        etUsername.isEnabled = false
                        etNIP.isEnabled = false
                        etRole.isEnabled = false
                    }
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Error decrypting/parsing user data", e)
                }
            },
            onLogout = { message ->
                handleLogout(message)
            }
        )
    }

    fun decryptField(obj: JSONObject, field: String, key: String): String {
        val value = obj.optString(field, "")
        return if (value.isNotEmpty() && value != "null") {
            try {
                CryptoAES.decrypt(value, key)
            } catch (e: Exception) {
                value
            }
        } else {
            ""
        }
    }

    private fun handleLogout(message: String) {
        when {
            message == "__TIMEOUT__" || message == "__NO_INTERNET__" -> {
                Toast.makeText(this, "Connection Time Out Error", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
        startActivity(Intent(this, Home::class.java))
        finish()
    }

    private fun saveProfile() {
        if (subdit.isNullOrEmpty()) {
            Toast.makeText(this, "Pilih Subdit", Toast.LENGTH_SHORT).show()
            return
        }

        val params = HashMap<String, String>()
        params["nama"] = binding.etNama.text.toString()
        params["pangkat"] = binding.etPangkat.text.toString()
        params["jabatan"] = binding.etJabatan.text.toString()
        params["subdit"] = subdit.toString()

        AuthManager(this, "api/simpanprofile").checkAuth(
            params = params,
            onLoading = { showLoading() },
            onSuccess = { json ->
                Log.e("json",json.toString())
                val status = json.getString("status")
                if(status=="ok"){
                    hideLoading()
                    Toast.makeText(this, "Profile berhasil diubah", Toast.LENGTH_SHORT).show()
                    finish()
                }

            },
            onLogout = { message ->
                hideLoading()
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun changePassword() {
        val passLama = binding.etPassLama.text.toString()
        val passBaru = binding.etPassBaru.text.toString()
        val konfirmasi = binding.etPassKonfirmasi.text.toString()

        if (passBaru.isEmpty() || passLama.isEmpty()) {
            Toast.makeText(this, "Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        if (passBaru != konfirmasi) {
            Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
            return
        }

        val params = HashMap<String, String>()
        params["old_password"] = passLama
        params["new_password"] = passBaru

        AuthManager(this, "api/ubahpassword").checkAuth(
            params = params,
            onLoading = { showLoading() },
            onSuccess = { _ ->
                hideLoading()
                Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_SHORT).show()

            },
            onLogout = { message ->
                hideLoading()
                handleLogout(message)
            }
        )
    }
}
