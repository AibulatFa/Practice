package com.example.practice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session
import org.neo4j.driver.exceptions.ClientException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoomList(this)
        }
    }

    @Composable
    fun RoomList(context: Context) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        Text(
            text = "Выберите свою комнату)",
            modifier = Modifier
                .padding(16.dp),
            style = MaterialTheme.typography.h4,
            color = Color.Black
        )
            Image(
                painter = painterResource(id = R.drawable.karta), // Замените "your_image" на ресурс вашего изображения
                contentDescription = null, // Отключите описание
                modifier = Modifier.fillMaxWidth()
            )

            val roomsWithNumbers = listOf(
            RoomInfo("Карта 1"),
            RoomInfo("Карта 2"),
            RoomInfo("Карта 3"),

        )

        // Отображаем каждую комнату с кнопкой "Play"
        Column {
            roomsWithNumbers.forEach { roomInfo ->
                RoomItem(roomInfo, context)
            }
        }
    }
    }

    data class RoomInfo(val roomName: String)

    @Composable
    fun RoomItem(roomInfo: RoomInfo, context: Context) {
        val purpleColor = Color(0xFF9B039B) // Фиолетовый цвет

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = roomInfo.roomName,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                style = MaterialTheme.typography.h6,
                color = Color.Black // Цвет текста
            )

            Button(
                onClick = {
                    createRoomIfNotExists(roomInfo)
                    val intent = Intent(context, Forms::class.java)
                    intent.putExtra("roomName", roomInfo.roomName)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = purpleColor, contentColor = Color.White), // Используем объявленный цвет
                shape = CircleShape, // Круглая форма кнопки
                modifier = Modifier.size(48.dp) // Размер кнопки
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null, // Отключите описание
                )
            }
        }
    }


    fun createRoomIfNotExists(roomInfo: RoomInfo) {
        Thread {
            try {
                val driver: Driver = GraphDatabase.driver(
                    "neo4j+ssc://95827caa.databases.neo4j.io:7687",
                    AuthTokens.basic("neo4j", "nSfGLYqjW78Qbam52GO6KVcia5Dvn8f-zpx2dsP5GEE")
                )
                // Создаем сессию и выполняем запрос для проверки существующей комнаты
                val session: Session = driver.session()
                val checkRoomQuery = """
                MATCH (room:Room {roomName: '${roomInfo.roomName}'})
                RETURN room
            """.trimIndent()
                val result = session.run(checkRoomQuery)
                val resultList = result.list()

                // Если результат запроса пустой, то создаем комнату
                if (resultList.isEmpty()) {
                    val createRoomQuery = """
                    CREATE (room:Room {roomName: '${roomInfo.roomName}'})
                """.trimIndent()
                    session.run(createRoomQuery)
                }

                session.close()
                driver.close()
            } catch (e: ClientException) {
                // Обработка ошибки, если не удалось выполнить операцию
            }
        }.start()
    }


}
