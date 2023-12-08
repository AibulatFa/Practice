package com.example.practice

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session
import org.neo4j.driver.exceptions.ClientException

class Neo4jDatabase {
    fun registerUser(
        firstName: String,
        email: String,
        password: String,
        roomName: String,
        latitude : String,
        longitude : String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val driver: Driver = GraphDatabase.driver(
                    "neo4j+ssc://95827caa.databases.neo4j.io:7687",
                    AuthTokens.basic("neo4j", "nSfGLYqjW78Qbam52GO6KVcia5Dvn8f-zpx2dsP5GEE")
                )
                // Создаем сессию и выполняем запрос для проверки существующего пользователя
                val session: Session = driver.session()
                val checkUserQuery = "MATCH (user:User {email: '$email', room: '$roomName'}) RETURN user"
                val result = session.run(checkUserQuery)
                val resultList = result.list()
                // Если результат запроса пустой, то регистрируем пользователя и связываем его с комнатой
                if (resultList.isEmpty()) {
                    val createUserQuery = """
                    CREATE (user:User {
                        firstName: '$firstName',
                        email: '$email',
                        password: '$password',
                        room: '$roomName',// Добавляем комнату к пользователю
                        latitude: '$latitude',
                        longitude: '$longitude'
                    })
                """.trimIndent()

                    session.run(createUserQuery)

                    // Создаем отношение (relationship) между пользователем и комнатой
                    val createRelationshipQuery = """
                    MATCH (user:User {email: '$email'}), (room:Room {roomName: '$roomName'})
                    CREATE (user)-[:MEMBER_OF]->(room)
                """.trimIndent()

                    session.run(createRelationshipQuery)

                    onSuccess.invoke() // Вызываем колбэк успешной регистрации
                } else {
                    onError.invoke("Пользователь с таким email уже существует.")
                }
                session.close()
                driver.close()
            } catch (e: ClientException) {
                onError.invoke("Произошла ошибка при регистрации.")
            }
        }.start()
    }


    fun loginUser(
        email: String,
        password: String,
        roomName: String, // Новый параметр для указания комнаты
        latitude : String,
        longitude : String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val driver: Driver = GraphDatabase.driver(
                    "neo4j+ssc://95827caa.databases.neo4j.io:7687",
                    AuthTokens.basic("neo4j", "nSfGLYqjW78Qbam52GO6KVcia5Dvn8f-zpx2dsP5GEE")
                )
                // Создаем сессию и выполняем запрос для проверки существующего пользователя
                val session: Session = driver.session()
                val checkUserQuery = "MATCH (user:User {email: '$email', password: '$password', room: '$roomName'}) RETURN user"
                val result = session.run(checkUserQuery)
                val resultList = result.list()
                // Если результат запроса содержит пользователя, то вход успешен
                if (resultList.isNotEmpty()) {
                    // Обновляем координаты пользователя в базе данных
                    val updateUserQuery = "MATCH (user:User {email: '$email', password: '$password', room: '$roomName'}) SET user.latitude = $latitude, user.longitude = $longitude"
                    session.run(updateUserQuery)

                    onSuccess()
                } else {
                    onError.invoke("Неверный email или пароль")
                }

                session.close()
                driver.close()
            } catch (e: ClientException) {
                onError.invoke("Ошибка при входе")
            }
        }.start()
    }



}
