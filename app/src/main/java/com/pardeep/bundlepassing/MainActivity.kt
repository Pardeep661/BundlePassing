package com.pardeep.bundlepassing

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.pardeep.bundlepassing.databinding.ActivityMainBinding
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    var binding : ActivityMainBinding?= null
    lateinit var navController: NavController
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // -------------------- appBar setup ------------------
        setSupportActionBar(binding?.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolBar?.setNavigationOnClickListener {
            navController.navigateUp()
        }

        // -------------------- appBar setup ------------------

        // ------------------- Button navigation --------------
        navController = findNavController(R.id.fragmentData)
        binding?.bottomNavigation?.setOnItemSelectedListener{item ->
            when(item.itemId){
                R.id.realtimeDataFragment ->{
                    navController.navigate(R.id.realtimeDataFragment)

                }
                R.id.fireStore ->{
                    navController.navigate(R.id.firestoreFragment)
                }
            }
            return@setOnItemSelectedListener true
        }


        navController.addOnDestinationChangedListener{_,destination, _ ->
            println("${destination.id}")
            when(destination.id){
                R.id.realtimeDataFragment -> {
                    binding?.bottomNavigation?.menu?.get(0)?.setChecked(true)
                    binding?.toolBar?.setTitle("RTDB")
                }
                R.id.firestoreFragment -> {
                    binding?.bottomNavigation?.menu?.get(1)?.setChecked(true)
                    binding?.toolBar?.setTitle("Firestore")
                }
            }
        }
        // ------------------- Button navigation --------------


    }

    fun requstAndCheckPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    if (Environment.isExternalStorageManager()) {
                        //permission , granted
                    } else {
                        //For Android 10
                        if (ContextCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestForManageExternalPermission()
                        }
                    }

                }
            } else {
                //For Android 10 we need to read the external storage
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestForManageExternalPermission()
                }
            }
        }

    private fun requestForManageExternalPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent,100)
            }
            catch (e : ActivityNotFoundException){
                Log.e(
                    TAG,
                    "requestForManageExternalPermission: Activity not found for the permission intent",

                    )
            }
        }
        else{
            Log.e(TAG, "requestForManageExternalPermission: The permission only applicable on android 11")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            100 ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            101 ->{
                if (Environment.isExternalStorageManager()){
                    Toast.makeText(
                        this,
                        "Full storage Access Granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }else{
                    Toast.makeText(this, "Storage access denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}