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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.firestore
import com.pardeep.bundlepassing.databinding.FragmentFirestoreBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FirestoreFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FirestoreFragment : Fragment(),recyclerInterface {
    // TODO: Rename and change types of parameters
    lateinit var supabaseClient: SupabaseClient
    private var param1: String? = null
    private var param2: String? = null
    var binding : FragmentFirestoreBinding?=null
    var db = Firebase.firestore
    var collectionName = "StudentData"
    var studentArray = arrayListOf<StudentData>()
    var myAdp = MyAdp(studentArray , this , this)
    lateinit var linearLayoutManager: LinearLayoutManager
    var mainActivity : MainActivity?=null
    var state : Boolean = false
    var customImageView : ImageView?=null
    var imageUri : Uri?=null
    var imageUrl : String?=null
    private val TAG = "FirestoreFragment"
    lateinit var navController: NavController
    var update : Boolean = false


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
        binding = FragmentFirestoreBinding.inflate(layoutInflater)
        return binding?.root
        //return inflater.inflate(R.layout.fragment_firestore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
        supabaseClient = (mainActivity?.applicationContext as MyApplication).supabaseClient

        db.collection(collectionName).addSnapshotListener{ snapshots , e ->
            if (e!=null){
                return@addSnapshotListener
            }

            for (snapshot in snapshots!!.documentChanges){
                var studentDataModel = changeObject(snapshot.document)

                when(snapshot.type){
                    DocumentChange.Type.ADDED ->{
                        studentDataModel.let {
                            studentArray.add(it)
                        }
                    }
                    DocumentChange.Type.REMOVED ->{
                        studentDataModel.let {
                            var index = getIndex(it)
                            if (index >-1){
                                studentArray.removeAt(index)
                            }
                        }
                    }
                    DocumentChange.Type.MODIFIED ->{
                        studentDataModel.let {
                            var index = getIndex(it)
                            if (index > -1){
                                studentArray.set(index,studentDataModel)
                            }
                        }
                    }
                }
                myAdp.notifyDataSetChanged()
            }
        }

        binding?.fab?.setOnClickListener {
            Dialog(requireContext()).apply {
                setContentView(R.layout.custom_add_layout)
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
                val studentNameEt = findViewById<EditText>(R.id.studentNameEt)
                val studentAgeEt = findViewById<EditText>(R.id.studentAgeEt)
                val addBtn : Button = findViewById(R.id.AddBtn)
                customImageView = findViewById<ImageView>(R.id.customImageView)
                customImageView?.setOnClickListener {
                    mainActivity?.requstAndCheckPermission()
                    imagePicker()
                }

                addBtn.setOnClickListener {
                    if (studentNameEt.text.trim().isNullOrEmpty()){
                        studentNameEt.error ="Enter student name"
                    }else if(studentAgeEt.text.trim().isNullOrEmpty()){
                        studentAgeEt.error = "Enter student age"
                    }else{
                        state = true
                        stateChecker(imageUri!!)
                        Handler(Looper.getMainLooper()).postDelayed({
                            val studentName = studentNameEt.text.toString()
                            val studentAge = studentAgeEt.text.toString()
                            val newObject = StudentData(name = studentName, age = studentAge.toInt(), image = imageUrl )
                            db.collection(collectionName).add(newObject)
                            dismiss()
                            state = false
                            imageUrl = null
                            imageUri = null
                        },2000)

                    }
                }
            }.show()
        }

        binding?.fireStoreRecycler?.adapter = myAdp
        linearLayoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
        binding?.fireStoreRecycler?.layoutManager = linearLayoutManager

        myAdp.notifyDataSetChanged()

    }

    private fun imagePicker() {
        val intent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(intent,1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1 ){
            data?.data.let { uri->
                Toast.makeText(requireContext(), "$uri", Toast.LENGTH_SHORT).show()
                imageUri = uri
                customImageView?.setImageURI(uri)
                uri?.let {
                    stateChecker(it)
                }

                myAdp.notifyDataSetChanged()
            }
        }
    }

    private fun stateChecker(it: Uri) {
        if (state){
            uploadToSupaBase(it)
        }
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

    private fun convertToByte(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        return inputStream?.readBytes()?:ByteArray(0)
    }

    private fun getIndex(it: StudentData): Int {
        var index = -1
         index = studentArray.indexOfFirst { element ->
            element.id.equals(it.id)==true
        }
        return index
    }

    private fun changeObject(document: QueryDocumentSnapshot): StudentData {
        val studentDataModel : StudentData = document.toObject(StudentData::class.java)
        if (studentDataModel!=null){
            studentDataModel.id = document.id?:""
        }
        return studentDataModel
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FirestoreFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FirestoreFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onItemClick(position: Int) {
        openCardView(position)
    }

    private fun openCardView(position: Int) {
        navController.navigate(R.id.detailedFragment, bundleOf(
            "studentName" to studentArray[position].name,
            "age" to studentArray[position].age,
            "imageUrl" to studentArray[position].image
        ))
    }

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

    private fun updateFunctionality(position: Int) {
        Dialog(requireContext()).apply {
            setContentView(R.layout.custom_add_layout)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            var studentNameEt = findViewById<EditText>(R.id.studentNameEt)
            var studentAgeEt = findViewById<EditText>(R.id.studentAgeEt)
            var addBtn : Button = findViewById(R.id.AddBtn)
            customImageView = findViewById(R.id.customImageView)

            Glide.with(requireContext())
                .load(studentArray[position].image)
                .into(customImageView!!)

            customImageView?.setOnClickListener {
                mainActivity?.requstAndCheckPermission()
                imagePicker()
            }
            studentAgeEt.setText(studentArray[position].age.toString())
            studentNameEt.setText(studentArray[position].name)
            addBtn.setText("Update")

            addBtn.setOnClickListener {
                if (studentNameEt.text.trim().isNullOrEmpty()){
                    studentNameEt.error ="Enter student name"
                }else if(studentAgeEt.text.trim().isNullOrEmpty()){
                    studentAgeEt.error = "Enter student age"
                }else{
                    state = true
                    stateChecker(imageUri!!)
                    Handler(Looper.getMainLooper()).postDelayed({
                        val id = studentArray[position].id
                        var studentName = studentNameEt.text.toString()
                        var studentAge = studentAgeEt.text.toString()
                        val updateObject = StudentData(id = id  ,name = studentName, age = studentAge.toInt(), image = imageUrl)
                        db.collection(collectionName).document(id.toString()).set(updateObject)
                        dismiss()
                        state = false
                    },2000)
                }
            }
        }.show()
    }

    private fun deletefunctionlity(position: Int) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Do you want delete")
            setPositiveButton("Yes"){ _,_ -> 
                db.collection(collectionName).document(studentArray[position].id.toString()).delete()
            }
            setNegativeButton("No"){dialog,_ ->
                dialog.dismiss()
            }
        }.show()  
    }

    override fun onPause() {
        super.onPause()
        myAdp.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        myAdp.notifyDataSetChanged()
    }

}
