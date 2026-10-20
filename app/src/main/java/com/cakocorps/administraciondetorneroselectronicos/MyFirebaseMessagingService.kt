package com.cakocorps.administraciondetorneroselectronicos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Se ejecuta cuando el token de este celular cambia o se crea por primera vez
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Nuevo token: $token")
    }

    // Se ejecuta cuando la app ESTÁ ABIERTA y llega un mensaje
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extraemos el título y contenido que enviaste desde la consola de Firebase
        val titulo = remoteMessage.notification?.title ?: "Notificación de Torneo"
        val cuerpo = remoteMessage.notification?.body ?: "Tienes un nuevo mensaje"

        mostrarNotificacionLocal(titulo, cuerpo)
    }

    private fun mostrarNotificacionLocal(titulo: String, cuerpo: String) {
        val channelId = "canal_torneos"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el "Canal de Notificaciones" (Es un requisito obligatorio para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                channelId,
                "Notificaciones de Torneos",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(canal)
        }

        // Construir la ventana visual de la notificación
        val notificacion = NotificationCompat.Builder(this, channelId)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setSmallIcon(R.mipmap.ic_launcher) // Usamos el ícono por defecto de tu app
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Desaparece al tocarla
            .build()

        // Le pedimos al sistema Android que la muestre en pantalla
        notificationManager.notify(0, notificacion)
    }
}