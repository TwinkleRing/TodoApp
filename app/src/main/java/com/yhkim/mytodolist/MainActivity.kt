package com.yhkim.mytodolist

import android.graphics.Paint
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yhkim.mytodolist.databinding.ActivityMainBinding
import com.yhkim.mytodolist.databinding.TodoItemViewBinding
import java.lang.reflect.Type

class MainActivity : AppCompatActivity() {

    private val viewModel : MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        viewModel.addTodo(Todo("숙제"))

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

}

data class Todo(
    val text : String,
    var isDone : Boolean = false,
)

class TodoAdapter(private var dataSet: List<Todo>,
                  val onClickDeleteIcon : (todo :Todo) -> Unit,
                  val onClickItem : (todo :Todo) -> Unit
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
        holder.binding.todoText.text = todo.text

        if (todo.isDone) {
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

    fun setData(newData : List<Todo>) {
        dataSet = newData
        notifyDataSetChanged()
    }

}


class MainViewModel: ViewModel() {
    val todoLiveData = MutableLiveData<List<Todo>>()
    private val data = arrayListOf<Todo>()


    fun addTodo(todo: Todo) {
        data.add(todo)
        todoLiveData.value = data
    }

    fun deleteTodo(todo : Todo) {
        data.remove(todo)
        todoLiveData.value = data
    }

    fun toggleTodo(todo: Todo) {
        todo.isDone = !todo.isDone
        todoLiveData.value = data

    }
}
















