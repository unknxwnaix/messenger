import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.messenger.R

class MessageSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Получение информации о сообщении
        val extras = intent.extras
        val message = extras?.getString("message")

        // Отправка уведомления
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "channelId")
            .setContentTitle("Сообщение отправлено")
            .setContentText(message)
            .setSmallIcon(R.drawable.icon)
            .build()
        notificationManager.notify(0, notification)
    }
}