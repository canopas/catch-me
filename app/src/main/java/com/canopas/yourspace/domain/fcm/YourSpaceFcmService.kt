package com.canopas.yourspace.domain.fcm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.canopas.yourspace.R
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

const val CHANNEL_YOURSPACE = "notification_channel_your_space"
const val NOTIFICATION_ID = 101

private const val NOTIFICATION_TYPE_CHAT = "chat"


const val REPLY_ACTION_REQUEST_CODE = 102

@AndroidEntryPoint
class YourSpaceFcmService : FirebaseMessagingService() {
    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var notificationManager: NotificationManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        userPreferences.isFCMRegistered = false
        if (userPreferences.currentUser != null) {
            FcmRegisterWorker.startService(applicationContext)
        }
    }
//ignTqy6LEox7X8bDkJK2
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.e("XXX: onMessageReceived: data ${message.data}")
        val notification = message.notification
        notification?.let {
            val title = notification.title
            val body = notification.body
            val profile = message.data["senderProfileUrl"]
            val type = message.data["type"]

            if (title != null && body != null && type == NOTIFICATION_TYPE_CHAT) {
                scope.launch {
                    val bitmap =
                        if (profile.isNullOrEmpty()) null else getTrackBitmapFromUrl(profile)
                    sendNotification(this@YourSpaceFcmService, title, body, message.data, bitmap)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(
        context: Context,
        title: String,
        body: String,
        data: MutableMap<String, String>,
        profile: Bitmap?
    ) {
        val isGroup = data["isGroup"].toBoolean()
        val groupName = data["groupName"]
        val senderId = data["senderId"]
        val threadId = data["threadId"]
        val notificationId = threadId?.hashCode() ?: NOTIFICATION_ID

        val user = Person.Builder().apply {
            setKey(senderId)
            setName(title)
            profile?.let { setIcon(IconCompat.createWithAdaptiveBitmap(it)) }
        }.build()

        val style =
            findActiveNotification(this, notificationId)
                ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
                ?: NotificationCompat.MessagingStyle(user)

        style.also {
            if (isGroup) {
                it.setGroupConversation(true)
                it.setConversationTitle(groupName)
            }
            it.addMessage(body, System.currentTimeMillis(), user)
        }

        val nBuilder =
            context.messageNotificationBuilder(notificationId, threadId, title, body, style)
        notificationManager.notify(notificationId, nBuilder.build())
    }

    private suspend fun getTrackBitmapFromUrl(
        url: String,
    ): Bitmap? {
        try {
            val size = resources.getDimensionPixelSize(R.dimen.size_notification_large)

            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .size(size, size)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap
            return bitmap
        } catch (throwable: Throwable) {
            return null
        }
    }
}

