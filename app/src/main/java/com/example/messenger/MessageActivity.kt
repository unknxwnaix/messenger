package com.example.messenger

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.storage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

const val REQUEST_CODE = 200

class MessageActivity : AppCompatActivity() {

    lateinit var messageET: EditText
    lateinit var recyclerView: RecyclerView
    private var friendId = ""
    private var myId = ""

    private var permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false
    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var fileName = ""

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)

        recyclerView = findViewById(R.id.recycler)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }

        messageET = findViewById(R.id.messageEd)

        friendId = intent.getStringExtra("friend").toString()
        myId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        FriendHelper().getMessagesForSender(myId!!, friendId!!) { messages ->
            val adapter = MessengerAdapter(messages, myId, this)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
        }

        val sendBtn = findViewById<ImageButton>(R.id.sendMessage)
        val fileBtn = findViewById<ImageButton>(R.id.sendFile)
        val voiceBtn = findViewById<ImageButton>(R.id.sendVoice)

        sendBtn.setOnClickListener {
            sendMessage(myId, friendId, messageET.text.toString())
            messageET.setText("")

// Получение сообщений после отправки
            FriendHelper().getMessagesForSender(myId, friendId) { messages ->
                (recyclerView.adapter as MessengerAdapter).updateMessages(messages)
                recyclerView.smoothScrollToPosition(messages.size - 1) // Прокрутка к новому сообщению
            }
        }

        fileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            try {
                startActivityForResult(Intent.createChooser(intent, "Выберите файл"), 100)
            } catch (exception: Exception) {
            }
        }

        voiceBtn.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecording()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Действие при отпускании кнопки
                    // Останавливаем запись аудио
                    recorder.stop()
                    recorder.release()

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Отправка записи")
                    builder.setMessage("Хотите отправить данную запись?")
                    builder.setPositiveButton("Да") { dialog, which ->
                        // Загрузка записи в хранилище Firebase
                        val storageRef = Firebase.storage.reference
                        val fileUri = Uri.fromFile(File("$dirPath$fileName.mp3"))
                        val fileRef = storageRef.child("audio/$fileName.mp3")

                        fileRef.putFile(fileUri)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Запись успешно сохранена", Toast.LENGTH_SHORT).show()
                                // Удалить локальный файл после загрузки
                                fileRef.downloadUrl.addOnSuccessListener { uri ->
                                    sendMessage(myId, friendId, uri.toString())
                                    messageET.setText("")

// Получение сообщений после отправки
                                    FriendHelper().getMessagesForSender(myId, friendId) { messages ->
                                        (recyclerView.adapter as MessengerAdapter).updateMessages(
                                            messages
                                        )
                                        recyclerView.smoothScrollToPosition(messages.size - 1) // Прокрутка к новому сообщению
                                    }
                                }
                                File("$dirPath$fileName.mp3").delete()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this@MessageActivity,
                                    "Ошибка загрузки записи: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    builder.setNegativeButton("Нет") { dialog, which ->
                        // Удалить локальный файл без сохранения
                        File("$dirPath$fileName.mp3").delete()
                    }
                    builder.show()

                    true
                }
                else -> false
            }
        }


    }

    private fun sendMessage(senderId: String, receiverId: String, text: String) {

        FriendHelper().sendMessageToFriend(senderId, receiverId, text)
        val intent = Intent()
        intent.putExtra("message", "Новое сообщение")
        intent.action = "com.example.MESSAGE_SENT"
        sendBroadcast(intent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "channelId"
            val channel = NotificationChannel(channelId, "Channel Name", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Сообщение отправлено")
                .setContentText(text)
                .setSmallIcon(R.drawable.icon)
                .build()
            notificationManager.notify(0, notification)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val fileUri: Uri? = data.data
            val type = contentResolver.getType(fileUri!!) // Получение типа файла
            if (type != null && (type.contains("pdf") || type.contains("png") || type.contains("docx") || type.contains(
                    "jpeg"
                ))
            ) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Отправить файл?")
                builder.setMessage("Вы уверены, что хотите отправить выбранный файл?")
                builder.setPositiveButton("Отправить") { _, _ ->
                    val storageRef = Firebase.storage.reference
                    val fileName =
                        System.currentTimeMillis().toString() // Генерация уникального имени файла
                    val fileRef =
                        storageRef.child("files/$fileName") // Добавление уникального имени к пути файла
                    fileRef.putFile(fileUri)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@MessageActivity,
                                "Файл загружен",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Получить ссылку на загруженное изображение
                            fileRef.downloadUrl.addOnSuccessListener { uri ->
                                sendMessage(myId, friendId, uri.toString())
                                messageET.setText("")

// Получение сообщений после отправки
                                FriendHelper().getMessagesForSender(myId, friendId) { messages ->
                                    (recyclerView.adapter as MessengerAdapter).updateMessages(
                                        messages
                                    )
                                    recyclerView.smoothScrollToPosition(messages.size - 1) // Прокрутка к новому сообщению
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this@MessageActivity,
                                "Ошибка загрузки файла: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                builder.setNegativeButton("Отмена") { _, _ ->
                    builder.setCancelable(true)
                }
                builder.show()
            } else {
                // Обработка недопустимого типа файла
                Toast.makeText(this, "Недопустимый тип файла", Toast.LENGTH_SHORT).show()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty()) {
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun startRecording(){
        if (!permissionGranted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        fileName = "audio_record$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")
            Log.d("d", "$dirPath$fileName.mp3")
            try {
                recorder.prepare()
                recorder.start()
            } catch (e: IOException){
                Log.e("Recording", "Ошибка при запуске записи: ${e.message}")
            }
        }
    }

}