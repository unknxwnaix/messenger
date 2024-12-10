package com.example.messenger

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.net.URL

class MessengerAdapter(private var messages: List<Message>, private val currentUserId: String, private val context: Context) : RecyclerView.Adapter<MessengerAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        // Заполнение данных для отображения в карточке сообщения
        holder.senderName.text = if (message.senderId == currentUserId) "Вы" else "Пользователь"
        if (message.senderId == currentUserId) {
            holder.layout.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.colorSentMessageBackground))
        } else {
            holder.layout.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.colorReceivedMessageBackground))
        }

        // Проверяем, содержит ли текст сообщения ссылку на Firebase Storage
        if (message.text!!.contains("https://firebasestorage.googleapis.com") && message.text!!.contains("files")) {
            holder.messageText.text = "Файл"
            holder.messageText.setOnClickListener {
                showDownloadDialog(message.text) // Показываем диалоговое окно для скачивания файла
            }
        } else if (message.text!!.contains("https://firebasestorage.googleapis.com") && message.text!!.contains("audio")){
            holder.messageText.text = "Голосовое сообщение"
            holder.messageText.setOnClickListener {
                playAudioFromFirebase(message.text) // Проигрываем аудио по нажатию на текст сообщения
            }
        }
        else {
            holder.messageText.text = message.text
        }

        holder.messageDate.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(
            Date(message.timestamp)
        )

        val imageRef = Firebase.storage.reference.child("images/${message.senderId}")
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            // Загрузка изображения с помощью Picasso и установка его в CircleImageView
            Picasso.get().load(uri).into(holder.avatar)
        }.addOnFailureListener {
            Picasso.get().load(R.drawable.user).into(holder.avatar)
        }

        holder.itemView.setOnClickListener {
            val options = arrayOf("Удалить для себя", "Удалить для всех")

            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Удаление сообщения")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        FriendHelper().deleteMessageByContent(message.senderId.toString(), message.receiverId.toString(), message.text.toString())
                        notifyItemRemoved(position)
                    }
                    1 -> {
                        FriendHelper().deleteMessageByContentAll(message.senderId.toString(), message.receiverId.toString(), message.text.toString())
                        notifyItemRemoved(position)
                    }
                }
            }
            builder.show()
        }

    }

    override fun getItemCount(): Int {
        return messages.size
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView = itemView.findViewById(R.id.nameTV)
        val messageText: TextView = itemView.findViewById(R.id.messageTV)
        val messageDate: TextView = itemView.findViewById(R.id.dateTV)
        val avatar: CircleImageView = itemView.findViewById(R.id.avatar_image)
        val layout: ConstraintLayout = itemView.findViewById(R.id.layoutMessage)

    }
    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    private fun showDownloadDialog(fileUrl: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Скачать файл")
        builder.setMessage("Вы уверены, что хотите скачать файл?")
        builder.setPositiveButton("Скачать") { _, _ ->

            val storageReference = Firebase.storage.getReferenceFromUrl(fileUrl)
            storageReference.metadata.addOnSuccessListener { storageMetadata ->
                val contentType = storageMetadata.contentType
                val type = contentType?.split('/')
                // Теперь вы можете использовать contentType для определения типа файла
                Log.d("File Type", contentType!!)
                val localFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "downloaded_file.${type?.get(1)}") // Change to use getExternalFilesDir instead of getExternalStorageDirectory

                storageReference.getFile(localFile).addOnSuccessListener {
                    Toast.makeText(context, "Файл успешно скачан", Toast.LENGTH_SHORT).show()
                    Log.d("Downloaded File Path", localFile.absolutePath)
                }.addOnFailureListener {
                    Log.d("Downloaded File Error", it.toString())
                }
            }.addOnFailureListener {
                Log.d("File Type Error", it.toString())
            }


        }
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun playAudioFromFirebase(url: String){
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)

        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: IOException) {
            Log.e("AudioPlayer", "Ошибка при проигрывании аудио: ${e.message}")
        }
    }


}