package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.material3.Card
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val USERNAME = "admin"
private const val PASSWORD = "1234"
private const val SECONDS_PER_ERROR = 5
private const val ATTEMPTS_PER_ROUND = 3

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    var loggedIn by rememberSaveable { mutableStateOf(false) }
    if (loggedIn) CalculatorScreen(modifier)
    else LoginScreen(modifier, onLoginSuccess = { loggedIn = true })
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLoginSuccess: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var lockRounds by rememberSaveable { mutableIntStateOf(0) }
    var failedAttempts by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf("") }
    var lockUntil by rememberSaveable { mutableLongStateOf(0L) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(lockUntil) {
        while (true) {
            val msLeft = lockUntil - System.currentTimeMillis()
            remainingSeconds = max(0, ceil(msLeft / 1000.0).toInt())
            if (remainingSeconds == 0){
                message = "Ingresar"
                break
            }
            delay(250)
        }
    }

    val locked = remainingSeconds > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !locked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !locked,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        Button(
            onClick = {
                if (username == USERNAME && password == PASSWORD) onLoginSuccess()
                else {
                    failedAttempts++
                    if (failedAttempts >= ATTEMPTS_PER_ROUND) {
                        lockRounds++
                        val waitSeconds = SECONDS_PER_ERROR * lockRounds
                        lockUntil = System.currentTimeMillis() + waitSeconds * 1000L
                        failedAttempts = 0
                        message = "Espera $waitSeconds segundos."
                    } else {
                        val left = ATTEMPTS_PER_ROUND - failedAttempts
                        message = "Intentos restantes: $left"
                    }
                }
            },
            enabled = !locked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text(if (locked) "Espera ${remainingSeconds}s" else message)
        }
    }
}

@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    var num1 by rememberSaveable { mutableStateOf("") }
    var num2 by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SensorsPanel()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sumadora", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = num1,
                onValueChange = { num1 = it },
                label = { Text("Primer número") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )

            OutlinedTextField(
                value = num2,
                onValueChange = { num2 = it },
                label = { Text("Segundo número") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            Button(
                onClick = {
                    val a = num1.toDoubleOrNull()
                    val b = num2.toDoubleOrNull()
                    result = if (a == null || b == null) {
                        "Ingresa dos números válidos"
                    } else {
                        val sum = a + b
                        "Resultado: " + if (sum % 1.0 == 0.0) sum.toLong().toString() else sum.toString()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text("Sumar")
            }

            if (result.isNotEmpty()) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}

@Composable
fun SensorsPanel() {
    val context = LocalContext.current
    var accel by remember { mutableStateOf("Esperando…") }
    var gyro by remember { mutableStateOf("Esperando…") }
    var light by remember { mutableStateOf("Esperando…") }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER ->
                        accel = "x=%.2f  y=%.2f  z=%.2f m/s²".format(
                            event.values[0], event.values[1], event.values[2]
                        )
                    Sensor.TYPE_GYROSCOPE ->
                        gyro = "x=%.2f  y=%.2f  z=%.2f rad/s".format(
                            event.values[0], event.values[1], event.values[2]
                        )
                    Sensor.TYPE_LIGHT ->
                        light = "%.1f lx".format(event.values[0])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        fun register(type: Int, onMissing: () -> Unit) {
            val sensor = sm.getDefaultSensor(type)
            if (sensor == null) onMissing()
            else sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        register(Sensor.TYPE_ACCELEROMETER) { accel = "No disponible" }
        register(Sensor.TYPE_GYROSCOPE) { gyro = "No disponible" }
        register(Sensor.TYPE_LIGHT) { light = "No disponible" }

        onDispose { sm.unregisterListener(listener) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sensores", style = MaterialTheme.typography.titleMedium)
            Text("Acelerómetro: $accel", modifier = Modifier.padding(top = 8.dp))
            Text("Giroscopio: $gyro", modifier = Modifier.padding(top = 4.dp))
            Text("Luz: $light", modifier = Modifier.padding(top = 4.dp))
        }
    }
}
