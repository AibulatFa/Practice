package com.example.practice

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.practice.ui.theme.PracticeTheme
import kotlinx.coroutines.launch
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session
import org.neo4j.driver.exceptions.ClientException

class LC : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PracticeTheme {
                // Вызовите свой пользовательский Composable-компонент
                MyContent()
            }
        }
    }

    @Composable
    fun MyContent() {
        var newPassword by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Ваш текст
            Text("Room: ${Viewer.roo}", color = Color.Black, style = MaterialTheme.typography.body1, textAlign = TextAlign.Center)
            Text("Email: ${Viewer.mail}", color = Color.Black, style = MaterialTheme.typography.body1, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Новый пароль") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки "Сохранить" и "Назад" на одной линии
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        // Вызов функции для изменения пароля
                        changePassword(newPassword)
                        Toast.makeText(this@LC, "Password has been changed.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить пароль")
                }
                Spacer(modifier = Modifier.width(16.dp)) // Добавляем отступ между кнопками
                Button(
                    onClick = {
                        finish()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Назад")
                }
            }
        }
    }


    fun changePassword(newPassword: String) {
        val email = Viewer.mail
        val room = Viewer.roo
        Thread {
            try {
                val driver: Driver = GraphDatabase.driver(
                    "neo4j+ssc://95827caa.databases.neo4j.io:7687",
                    AuthTokens.basic("neo4j", "nSfGLYqjW78Qbam52GO6KVcia5Dvn8f-zpx2dsP5GEE")
                )

                val session: Session = driver.session()

                // Выполните запрос для обновления пароля пользователя
                val query = """
                MATCH (u:User {email: '$email',room: '$room'})
                SET u.password = '$newPassword'
            """

                session.run(query)

                session.close()
                driver.close()
            } catch (e: ClientException) {
            }
        }.start()
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        PracticeTheme {
            MyContent()
        }
    }
}


