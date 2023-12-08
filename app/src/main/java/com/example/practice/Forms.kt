package com.example.practice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session
import org.neo4j.driver.exceptions.ClientException
import kotlin.collections.Map

class Forms: ComponentActivity() {

    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScr()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun MainScr(){
        var tabIndex = rememberSaveable { mutableStateOf(0) }
        val pagerState = rememberPagerState()
        val scope = rememberCoroutineScope()
        val tabTitles = listOf<String>("Login", "Register")
        val white = Color(0xffffffff)
        val ash7a = Color(0xFFBB86FC)
        val pearl = Color(0xFF6200EE)
        val neo4jDatabase = Neo4jDatabase()

        Column {
            TabRow(selectedTabIndex = tabIndex.value,
                backgroundColor = colorResource(id = R.color.purple_200),
                modifier = Modifier
                    .background(color = Color.Transparent),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier
                            .pagerTabIndicatorOffset(
                                pagerState,
                                tabPositions
                            )
                            .height(0.dp)
                            .size(0.dp)
                    )
                })
            {
                tabTitles.forEachIndexed { index, title ->
                    val tabColor = remember {
                        Animatable(white)
                    }

                    val textColor = remember {
                        Animatable(ash7a)
                    }

                    LaunchedEffect(key1 = pagerState.currentPage == index) {
                        tabColor.animateTo(if (pagerState.currentPage == index) pearl else white)
                        textColor.animateTo(if (pagerState.currentPage == index) white else ash7a)
                    }

                    Tab(
                        selected = pagerState.currentPage == index,
                        modifier = Modifier
                            .background(
                                color = tabColor.value
                            ),
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }) {
                        Text(
                            tabTitles[index],
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = TextStyle(
                                color = textColor.value,

                                )
                        )
                    }

                }
            }
            HorizontalPager(
                count = tabTitles.size,
                state = pagerState,
            ) { tabIndex ->
                if (tabIndex == 1) {
                    Register(neo4jDatabase, this@Forms)
                } else {
                    Login(neo4jDatabase, this@Forms)
                }
            }
        }
    }

    @SuppressLint("MissingPermission", "SuspiciousIndentation")
    @Composable
    fun Register(neo4jDatabase: Neo4jDatabase,context: Context) {
        var email by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        val roomName = intent.getStringExtra("roomName")
        val registrationState = remember { mutableStateOf<RegistrationState?>(null) }

        var latitude by remember { mutableStateOf("") }
        var longitude by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Регистрация",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("ФИО") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Подтвердждение Пароль") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (password == confirmPassword) {
                        if (roomName != null) {

                            if (ActivityCompat.checkSelfPermission(this@Forms, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(this@Forms, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                // Здесь можно получить координаты
                                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                if (location != null) {
                                    latitude = location.latitude.toString()
                                    longitude = location.longitude.toString()
                                }
                            } else {
                                // Запрос разрешений
                                ActivityCompat.requestPermissions(
                                    this@Forms,
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ),
                                    REQUEST_LOCATION_PERMISSION
                                )
                            }
                            Viewer.mail = email
                            Viewer.roo = roomName
                            Viewer.pass = password
                                neo4jDatabase.registerUser(
                                    firstName = name,
                                    email = email,
                                    password = password,
                                    roomName = roomName,
                                    latitude = latitude,
                                    longitude = longitude,
                                    onSuccess = {
                                        // Handle success
                                        registrationState.value = RegistrationState.Success
                                        val intent = Intent(context, com.example.practice.Map::class.java)
                                        context.startActivity(intent)
                                    },
                                    onError = {
                                        // Handle error
                                        registrationState.value = RegistrationState.Error(it)
                                    }
                                )
                        }
                    } else {
                        registrationState.value = RegistrationState.Error("Пароли не совпадают.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Icon(Icons.Filled.Person, contentDescription = "Войти")
                Text(text = "Регистрация")
            }

            registrationState.value?.let { state ->
                when (state) {
                    is RegistrationState.Success -> {
                        Text(text = "Регистрация успешна!", color = Color.Green)
                    }
                    is RegistrationState.Error -> {
                        Text(text = "Ошибка регистрации: ${state.errorMessage}", color = Color.Red)
                    }
                }

            }
        }
    }

    sealed class RegistrationState {
        object Success : RegistrationState()
        data class Error(val errorMessage: String) : RegistrationState()
    }

    @Composable
    fun Login(neo4jDatabase: Neo4jDatabase,context: Context) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val roomName = intent.getStringExtra("roomName")
        val loginState = remember { mutableStateOf<LoginState?>(null) }
        var latitude by remember { mutableStateOf("") }
        var longitude by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Логин",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (roomName != null) {

                        if (ActivityCompat.checkSelfPermission(this@Forms, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this@Forms, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            // Здесь можно получить координаты
                            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            if (location != null) {
                                latitude = location.latitude.toString()
                                longitude = location.longitude.toString()
                            }
                        } else {
                            // Запрос разрешений
                            ActivityCompat.requestPermissions(
                                this@Forms,
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ),
                                REQUEST_LOCATION_PERMISSION
                            )
                        }

                        Viewer.mail = email
                        Viewer.roo = roomName
                        Viewer.pass = password
                        neo4jDatabase.loginUser(
                            email = email,
                            password = password,
                            roomName = roomName,
                            latitude = latitude,
                            longitude = longitude,
                            onSuccess = {
                                loginState.value = LoginState.Success
                                val intent = Intent(context, com.example.practice.Map::class.java)
                                context.startActivity(intent)
                            },
                            onError = {
                                loginState.value = LoginState.Error(it)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Icon(Icons.Filled.Person, contentDescription = "Войти")
                Text(text = "Вход")
            }

            // Обработка состояния входа
            loginState.value?.let { state ->
                when (state) {
                    is LoginState.Success -> {
                        Text(text = "Вход успешен!", color = Color.Green)
                    }
                    is LoginState.Error -> {
                        Text(text = "Ошибка входа: ${state.errorMessage}", color = Color.Red)
                    }
                }
            }
        }
    }

    sealed class LoginState {
        object Success : LoginState()
        data class Error(val errorMessage: String) : LoginState()
    }
}