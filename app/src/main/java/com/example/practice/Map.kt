package com.example.practice

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session
import java.util.*

class Map : ComponentActivity() {
    private val MAPKIT_API_KEY = "ba1e4ee2-a331-4269-8e6b-085954773601"
    private var mapView: MapView? = null

    // Создаем коллекцию для хранения сообщений
    private val messagesList = LinkedList<String>()
    private lateinit var messageTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        MapKitFactory.setApiKey(MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
        // Создание MapView
        setContentView(R.layout.map)
        super.onCreate(savedInstanceState)
        mapView = findViewById(R.id.mapview)

        val email = Viewer.mail // Получаем email из Viewer
        val roomName = Viewer.roo // Получаем roomName из Viewer

        // Получаем координаты и перемещаем камеру на карту
        getCoordinatesByEmailAndRoo(email, roomName)

        getallCoordinatesByEmailAndRoo(email, roomName)
        // Получаем ссылки на виджеты для работы с сообщениями
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val Button2 = findViewById<Button>(R.id.secondButton)
        messageTextView = findViewById(R.id.messageTextView)

        // Устанавливаем обработчик нажатия кнопки для отправки сообщения
        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString()
            if (messageText.isNotEmpty()) {
                // Отправляем сообщение в Neo4j
                sendMessage(email, roomName, messageText) // Сначала отправляем сообщение

                // Очищаем поле ввода
                messageEditText.text.clear()
            }
        }

        Button2.setOnClickListener {
            val intent = Intent(this@Map, LC::class.java)
            this@Map.startActivity(intent)
        }

        // Обновляем textField
        updateMessageTextField(messageTextView, roomName)
    }

    override fun onStop() {
        // Вызов onStop нужно передавать инстансам MapView и MapKit.
        mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        // Вызов onStart нужно передавать инстансам MapView и MapKit.
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView?.onStart()
    }

    private fun getCoordinatesByEmailAndRoo(email: String, roomName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val driver: Driver = GraphDatabase.driver(
                "neo4j+ssc://95827caa.databases.neo4j.io:7687",
                AuthTokens.basic("neo4j", "nSfGLYqjW78Qbam52GO6KVcia5Dvn8f-zpx2dsP5GEE")
            )

            try {
                val session: Session = driver.session()
                val getCoordinatesQuery = """
                    MATCH (user:User {email: '$email', room: '$roomName'})
                    RETURN user.latitude AS latitude, user.longitude AS longitude
                """.trimIndent()

                val result = session.run(getCoordinatesQuery)
                if (result.hasNext()) {
                    val record = result.single()
                    val latitude = record.get("latitude").asDouble()
                    val longitude = record.get("longitude").asDouble()
                    session.close()
                    driver.close()

                    withContext(Dispatchers.Main) {
                        mapView?.map?.move(
                            CameraPosition(Point(latitude, longitude), 12.0f, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 5F),
                            null
                        )
                    }
                } else {
                    session.close()
                    driver.close()

                }
            } catch (e: Exception) {

            }
        }
    }

    data class UserLocation(val latitude: Double, val longitude: Double)

    private fun getallCoordinatesByEmailAndRoo(email: String, roomName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val driver: Driver = GraphDatabase.driver(
                "neo4j+ssc://95827caa.databases.neo4j.io:7687",
                AuthTokens.basic("neo4j", "nSfGLYqjW78Qbam52GO6KVcia5Dvn8f-zpx2dsP5GEE")
            )

            try {
                val session: Session = driver.session()
                val getCoordinatesQuery = """
                MATCH (user:User {room: '$roomName'})
                RETURN user.latitude AS latitude, user.longitude AS longitude
            """.trimIndent()

                val result = session.run(getCoordinatesQuery)
                val userLocations = mutableListOf<UserLocation>()

                while (result.hasNext()) {
                    val record = result.next()
                    val latitude = record.get("latitude").asDouble()
                    val longitude = record.get("longitude").asDouble()
                    userLocations.add(UserLocation(latitude, longitude))
                }

                session.close()
                driver.close()

                withContext(Dispatchers.Main) {
                    showUsersOnMa(userLocations, mapView)
                }
            } catch (e: Exception) {

            }
        }
    }

    private fun showUsersOnMa(userLocations: List<UserLocation>, mapView: MapView?) {
        if (mapView == null || userLocations.isEmpty()) {
            return
        }

        val mapObjects = mapView.map.mapObjects
        mapObjects.clear() // Очистка маркеров на карте перед добавлением новых

        for (userLocation in userLocations) {
            val point = Point(userLocation.latitude, userLocation.longitude)
            val userPlacemark = mapObjects.addPlacemark(point)



            userPlacemark.setIcon(ImageProvider.fromResource(mapView.context, R.drawable.d333))


            // Добавим лог для отслеживания
            Log.d("Map", "Added user marker at: ${userLocation.latitude}, ${userLocation.longitude}")
        }
    }



    private fun sendMessage(email: String, roomName: String, messageText: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val driver: Driver = GraphDatabase.driver(
                "neo4j+ssc://95827caa.databases.neo4j.io:7687",
                AuthTokens.basic("neo4j", "nSfGLYqjW78Qbam52GO6KVcia5Dvn8f-zpx2dsP5GEE")
            )

            try {
                val session: Session = driver.session()

                // Создаем сообщение и связываем его с отправителем и комнатой
                val createMessageQuery = """
                MATCH (user:User {email: '$email', room: '$roomName'})
                CREATE (message:Message {
                    text: '$messageText',
                    timestamp: timestamp()
                })
                MERGE (user)-[:SENT]->(message)
                MERGE (room:Room {name: '$roomName'})-[:CONTAINS]->(message)
            """.trimIndent()

                session.run(createMessageQuery)

                session.close()
                driver.close()

                // После успешной отправки сообщения обновляем текстовое поле
                updateMessageTextField(messageTextView, roomName)
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }


    private fun updateMessageTextField(textView: TextView, roomName: String) {
        messagesList.clear()
        lifecycleScope.launch(Dispatchers.IO) {
            val driver: Driver = GraphDatabase.driver(
                "neo4j+ssc://95827caa.databases.neo4j.io:7687",
                AuthTokens.basic("neo4j", "nSfGLYqjW78Qbam52GO6KVcia5Dvn8f-zpx2dsP5GEE")
            )

            try {
                val session: Session = driver.session()
                val getMessagesQuery = """
                MATCH (room:Room {name: '$roomName'})-[:CONTAINS]->(message:Message)<-[:SENT]-(user:User)
                RETURN user.email AS userEmail, message.text AS messageText
            """.trimIndent()

                val result = session.run(getMessagesQuery)
                val messages = mutableListOf<String>()

                while (result.hasNext()) {
                    val record = result.next()
                    val userEmail = record.get("userEmail").asString()
                    val messageText = record.get("messageText").asString()
                    val fullMessage = "$userEmail: $messageText"
                    messages.add(fullMessage)
                }

                session.close()
                driver.close()

                withContext(Dispatchers.Main) {
                    // Добавляем новые сообщения в начало списка
                    messagesList.addAll(0, messages)

                    // Обновляем textField
                    val messageText = messagesList.joinToString("\n")
                    textView.text = messageText
                }
            } catch (e: Exception) {

            }
        }
    }

}

