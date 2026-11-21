package com.example.trihb.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Leitura reativa do acelerômetro físico (Sensor.TYPE_ACCELEROMETER) para
 * contagem de passos / movimento ativo.
 *
 *  magnitude = sqrt(x² + y² + z²)
 *  delta     = |magnitude − 9.81|
 *  passo     = delta > 2.2  &&  intervalo desde o último passo > 350 ms (debounce)
 *
 * O registro/liberação do listener acontece em onResume/onPause através de um
 * DisposableEffect ligado ao ciclo de vida (ver TriHubNavGraph).
 */
class ActivitySensorManager private constructor(context: Context) : SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    private val acelerometro: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _passosDetectados = MutableStateFlow(0)
    val passosDetectados: StateFlow<Int> = _passosDetectados.asStateFlow()

    private val _sensorAtivo = MutableStateFlow(false)
    /** true enquanto o listener está registrado (app em primeiro plano). */
    val sensorAtivo: StateFlow<Boolean> = _sensorAtivo.asStateFlow()

    /** false em dispositivos/emuladores sem acelerômetro. */
    val sensorDisponivel: Boolean get() = acelerometro != null

    private var ultimoPassoMs = 0L

    fun iniciar() {
        val sensor = acelerometro ?: return
        if (_sensorAtivo.value) return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        _sensorAtivo.value = true
    }

    fun parar() {
        if (!_sensorAtivo.value) return
        sensorManager?.unregisterListener(this)
        _sensorAtivo.value = false
    }

    /** Contingência para demonstrações em emuladores sem movimento físico. */
    fun simularPassos(quantidade: Int = 20) {
        _passosDetectados.update { it + quantidade }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z)
        val delta = abs(magnitude - GRAVIDADE)

        val agora = SystemClock.elapsedRealtime()
        if (delta > LIMIAR_MOVIMENTO && agora - ultimoPassoMs > DEBOUNCE_MS) {
            ultimoPassoMs = agora
            _passosDetectados.update { it + 1 }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        const val GRAVIDADE = 9.81f
        const val LIMIAR_MOVIMENTO = 2.2f
        const val DEBOUNCE_MS = 350L

        @Volatile
        private var INSTANCE: ActivitySensorManager? = null

        fun getInstance(context: Context): ActivitySensorManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActivitySensorManager(context).also { INSTANCE = it }
            }
    }
}

/**
 * Regra de negócio do bônus de atividade física (requisito de hardware da AP2).
 * A cada 20 passos: +15 kcal na meta calórica e +25 ml na meta de hidratação.
 */
data class BonusAtividade(val passos: Int) {
    val blocos: Int get() = passos / PASSOS_POR_BLOCO
    val bonusCalorias: Int get() = blocos * KCAL_POR_BLOCO
    val bonusAguaMl: Int get() = blocos * AGUA_ML_POR_BLOCO
    /** Estimativa simples de gasto energético ativo (~0,04 kcal por passo). */
    val caloriasAtivasEstimadas: Int get() = (passos * 0.04f).toInt()
    /** Passos que faltam para o próximo bônus. */
    val passosParaProximoBonus: Int get() = PASSOS_POR_BLOCO - (passos % PASSOS_POR_BLOCO)

    companion object {
        const val PASSOS_POR_BLOCO = 20
        const val KCAL_POR_BLOCO = 15
        const val AGUA_ML_POR_BLOCO = 25
    }
}
