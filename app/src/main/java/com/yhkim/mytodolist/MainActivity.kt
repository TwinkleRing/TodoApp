package com.yhkim.mytodolist

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.model.DocumentSet
import com.google.firebase.ktx.Firebase
import com.yhkim.mytodolist.databinding.ActivityMainBinding
import com.yhkim.mytodolist.databinding.TodoItemViewBinding
import java.lang.reflect.Type

class MainActivity : AppCompatActivity() {

    val RC_SIGN_IN = 1000;

    private val viewModel : MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 로그인 안 됨
        if (FirebaseAuth.getInstance().currentUser == null) {
            login()
        }

        // viewModel.addTodo(Todo("숙제"))

        binding.RecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = TodoAdapter(
                emptyList(),
                onClickDeleteIcon = {
                    viewModel.deleteTodo(it)

            },
            onClickItem = {
                viewModel.toggleTodo(it)
            })
        }

        binding.addButton.setOnClickListener {
            val todo = Todo(binding.editText.text.toString())
            viewModel.addTodo(todo)
        }

        // 관찰 UI 업데이트
        viewModel.todoLiveData.observe(this, Observer {
            (binding.RecyclerView.adapter as TodoAdapter).setData(it)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                //val user = FirebaseAuth.getInstance().currentUser
                viewModel.fetchData()
            } else { // 로그인 실패
                finish() // 앱 꺼버린다.
            }
        }
    }

    fun login() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build())

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }

    fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                login()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater : MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_log_out -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}

data class Todo(
    val text : String,
    var isDone : Boolean = false,
)

class TodoAdapter(private var dataSet: List<DocumentSnapshot>,
                  val onClickDeleteIcon : (todo :DocumentSnapshot) -> Unit,
                  val onClickItem : (todo :DocumentSnapshot) -> Unit
) :
    RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    class ViewHolder(val binding: TodoItemViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.todo_item_view, viewGroup, false)

        return ViewHolder(TodoItemViewBinding.bind(view))
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = dataSet[position]
        holder.binding.todoText.text = todo.getString("text") ?: ""

        if ((todo.getBoolean("isDone") ?: false) == true)  {
            holder.binding.todoText.apply {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setTypeface(null, Typeface.ITALIC)
            }
        } else {
            holder.binding.todoText.apply {
                paintFlags = 0
                setTypeface(null, Typeface.NORMAL)
            }
        }

        holder.binding.delete.setOnClickListener {
            onClickDeleteIcon.invoke(todo)
        }

        holder.binding.root.setOnClickListener {
            onClickItem.invoke(todo)
        }
    }


    override fun getItemCount() = dataSet.size

    fun setData(newData : List<DocumentSnapshot>) {
        dataSet = newData
        notifyDataSetChanged()
    }
}


class MainViewModel: ViewModel() {
    val db = Firebase.firestore
    val todoLiveData = MutableLiveData<List<DocumentSnapshot>>()
    //private val data = arrayListOf<Todo>()

    init {
        fetchData()
    }

    fun fetchData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            db.collection(user.uid)

                ///// 실시간 데이터베이스 갱신
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }


                    if (value != null) {
                        todoLiveData.value = value.documents
                    }
                }
                //////

//                .get()
//                .addOnSuccessListener { result ->
//                    data.clear()
//                    for (document in result) {
//                        val todo = Todo(
//                            document.data["text"] as String,
//                            document.data["isDone"] as Boolean
//                        )
//                        data.add(todo)
//                    }
//                    todoLiveData.value = data
//
//                }
        }
    }

    fun addTodo(todo: Todo) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            db.collection(user.uid).add(todo)
        }

        //data.add(todo)
        //todoLiveData.value = data
    }

    fun deleteTodo(todo : DocumentSnapshot) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            db.collection(user.uid).document(todo.id).delete()
        }
    }

    fun toggleTodo(todo: DocumentSnapshot) {

        FirebaseAuth.getInstance().currentUser?.let { user ->
            val isDone = todo.getBoolean("isDone") ?: false
            db.collection(user.uid).document(todo.id).update("isDone",!isDone)
        }

        //todo.isDone = !todo.isDone
        //todoLiveData.value = data

    }
}


