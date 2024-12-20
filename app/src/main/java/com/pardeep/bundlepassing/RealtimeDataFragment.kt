package com.pardeep.bundlepassing

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database
import com.pardeep.bundlepassing.databinding.FragmentRealtimeDataBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RealtimeDataFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RealtimeDataFragment : Fragment(),recyclerInterface  {
    // TODO: Rename and change types of parameters
    lateinit var supabaseClient: SupabaseClient
    private var param1: String? = null
    private var param2: String? = null
    var binding : FragmentRealtimeDataBinding?=null
    var studentArray = arrayListOf<StudentData>()
    var myAdp = MyAdp(studentArray,this,this)
    lateinit var linearLayoutManager: LinearLayoutManager
    val fireBaseref = Firebase.database.getReference("RTDB")
    private val TAG = "RealtimeDataFragment"
    var mainActivity : MainActivity?=null
     var custom_imageView : ImageView?=null
    var state : Boolean = false
    var imageUri : Uri?=null
    var imageUrl : String= ""
    lateinit var navController : NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = activity as MainActivity
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRealtimeDataBinding.inflate(layoutInflater)
        return binding?.root
        //return inflater.inflate(R.layout.fragment_realtime_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
        supabaseClient = (mainActivity?.applicationContext as MyApplication).supabaseClient
        //----------------- firebase realtime dataBase --------------
        fireBaseref.addChildEventListener(object :ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val studentDataModel : StudentData ?=snapshot.getValue(StudentData::class.java)
                studentDataModel?.id = snapshot.key
                if (studentDataModel!=null){
                    studentArray.add(studentDataModel)
                    myAdp.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val studentDataModel : StudentData?=snapshot.getValue(StudentData::class.java)
                studentDataModel?.id = snapshot.key
                if (studentDataModel!=null){
                    studentArray.forEachIndexed { index, studentData ->
                        if (studentData.id == studentDataModel.id){
                            studentArray[index] = studentDataModel
                            myAdp.notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val studentDataModel : StudentData?=snapshot.getValue(StudentData::class.java)
                studentDataModel?.id = snapshot.key
                if (studentDataModel!=null){
                    studentArray.forEachIndexed { index, studentData ->
                        if (studentData.id == studentDataModel.id){
                            studentArray.removeAt(index)
                            myAdp.notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        //----------------- firebase realtime dataBase --------------
        binding?.recyclerView?.adapter = myAdp
        linearLayoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
        binding?.recyclerView?.layoutManager = linearLayoutManager

        binding?.fab?.setOnClickListener {
            Dialog(requireContext()).apply {
                setContentView(R.layout.custom_add_layout)
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
                val studentNameEt = findViewById<EditText>(R.id.studentNameEt)
                val studentAgeEt = findViewById<EditText>(R.id.studentAgeEt)
                val addBtn : Button = findViewById(R.id.AddBtn)
                custom_imageView = findViewById<ImageView>(R.id.customImageView)
                custom_imageView?.setOnClickListener {
                    mainActivity?.requstAndCheckPermission()
                    pickImage()

                }

                addBtn.setOnClickListener {
                    if (studentNameEt.text.trim().isEmpty()){
                        studentNameEt.error ="Enter student name"
                    }else if(studentAgeEt.text.trim().isEmpty()){
                        studentAgeEt.error = "Enter student age"
                    }else{
                        state = true
                        stateChecker(imageUri!!)
                        Handler(Looper.getMainLooper()).postDelayed({
                            val key = fireBaseref.push().key
                            val studentName = studentNameEt.text.toString()
                            val studentAge = studentAgeEt.text.toString()
                            val newObject = StudentData(id = key,name = studentName, age = studentAge.toInt(), image =imageUrl)
                            fireBaseref.child("$key").setValue(newObject)
                            state = false
                            dismiss()
                        },2000)
                    }
                }
            }.show()
        }

    }

    private fun pickImage() {
        startActivityForResult(
            Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            1
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode ==Activity.RESULT_OK && requestCode == 1 ){
            data?.data.let { uri->
                Toast.makeText(requireContext(), "$uri", Toast.LENGTH_SHORT).show()
                imageUri = uri.toString().toUri()
                custom_imageView?.setImageURI(uri)
                uri?.let {
                    stateChecker(it)
                }
            }
        }
    }

    private fun stateChecker(uri: Uri) {
        if (state){
            uploadToSupaBase(uri)
        }
    }

    private fun convertToByte(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        return inputStream?.readBytes()?:ByteArray(0)
    }

    private fun uploadToSupaBase(uri: Uri) {
        val bucket = supabaseClient.storage.from("RTDB_with_BundlePassing")
        val fileName = "upload/${System.currentTimeMillis()}.jpeg"
        val byteArray = convertToByte(requireContext(),uri)
        lifecycleScope.launch {
            try {
                bucket.uploadAsFlow(fileName,byteArray).collect{status ->
                    when(status){
                       is UploadStatus.Progress ->{
                           Log.d(TAG, "uploadToSupaBase: progress%")
                       } 
                        
                        is UploadStatus.Success ->{
                            imageUrl = bucket.publicUrl(fileName)
                            Toast.makeText(requireContext(), "Upload successfully ${imageUrl}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }catch (e : Exception){
            withContext(Dispatchers.IO){
                Log.e(TAG, "uploadToSupaBase: upload Error ${e.message}", )
                Toast.makeText(requireContext(), "Upload Error", Toast.LENGTH_SHORT).show()
            }
        }
        }
    }


//    private fun requestForManageExternalPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//            try {
//                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                startActivityForResult(intent,PERMISSION_REQUEST_CODE)
//            }
//            catch (e : ActivityNotFoundException){
//                Log.e(
//                    TAG,
//                    "requestForManageExternalPermission: Activity not found for the permission intent",
//
//                    )
//            }
//        }
//        else{
//            Log.e(TAG, "requestForManageExternalPermission: The permission only applicable on android 11")
//        }
//    }






    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RealtimeDataFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RealtimeDataFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    // -------------- recycler functionality----------------
    override fun onItemClick(position: Int) {
        navController.navigate(R.id.detailedFragment, bundleOf(
            "studentName" to studentArray[position].name,
            "age" to studentArray[position].age,
            "imageUrl" to studentArray[position].image
        )
        )
    studentArray.clear()
    myAdp.notifyDataSetChanged()}

    override fun operationClick(position: Int, callFor: String) {
        when(callFor){
            "delete" ->{
                deletefunctionlity(position)
            }
            "update" ->{
                updateFunctionality(position)
        }
    }

    }

    private fun deletefunctionlity(position: Int) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Do you want delete")
            setPositiveButton("Yes"){ _,_ ->
                fireBaseref.child("${studentArray[position].id}").removeValue()
            }
            setNegativeButton("No"){dialog,_ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun updateFunctionality(position: Int) {
                Dialog(requireContext()).apply {
                    setContentView(R.layout.custom_add_layout)
                    window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
                    val studentNameEt = findViewById<EditText>(R.id.studentNameEt)
                    val studentAgeEt = findViewById<EditText>(R.id.studentAgeEt)
                    custom_imageView = findViewById<ImageView>(R.id.customImageView)
                    val addBtn : Button = findViewById(R.id.AddBtn)
                    studentAgeEt.setText(studentArray[position].age.toString())
                    studentNameEt.setText(studentArray[position].name)

                        Glide.with(requireContext())
                            .load(studentArray[position].image)
                            .into(custom_imageView!!)
                    

                    custom_imageView?.setOnClickListener {
                        mainActivity?.requstAndCheckPermission()
                        pickImage()
                        custom_imageView?.setImageURI(imageUri)
                    }
                    addBtn.setText("Update")
                    addBtn.setOnClickListener {
                        if (studentNameEt.text.trim().isEmpty()){
                            studentNameEt.error ="Enter student name"
                        }else if(studentAgeEt.text.trim().isEmpty()){
                            studentAgeEt.error = "Enter student age"
                        }else{
                            state = true
                            stateChecker(imageUri!!)
                            Handler(Looper.getMainLooper()).postDelayed({
                                val id = studentArray[position].id
                                val studentName = studentNameEt.text.toString()
                                val studentAge = studentAgeEt.text.toString()
                                val updateObject = StudentData(id = id  ,name = studentName, age = studentAge.toInt(), image = imageUrl)
                                val hashObject = updateObject.toMap()
                                fireBaseref.child("$id").setValue(hashObject)
                                state = false
                                dismiss()
                            },2000)

                        }
                    }
                }.show()

    }

    // -------------- recycler functionality----------------

    override fun onResume() {
        super.onResume()
        Toast.makeText(requireContext(), "resume", Toast.LENGTH_SHORT).show()
        myAdp.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        myAdp.notifyDataSetChanged()
    }
}