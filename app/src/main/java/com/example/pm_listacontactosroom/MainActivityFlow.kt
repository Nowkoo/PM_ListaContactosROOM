package com.example.pm_listacontactosroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.pm_listacontactosroom.ui.theme.PM_ListaContactosROOMTheme
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

// 1. Modelo de datos Room (Entidad)
@Entity(tableName = "contactos")
data class Contacto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val mail: String
)

// 2. DAO (Interfaz para interactuar con la base de datos)
@Dao
interface ContactoDao {
    @Query("SELECT * FROM contactos")
    fun getTodosContactos(): Flow<List<Contacto>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarContacto(contacto: Contacto)
    @Delete
    suspend fun eliminarContacto(contacto: Contacto)
}

// 3. Base de datos Room
@Database(entities = [Contacto::class], version = 1, exportSchema =
false)
abstract class ContactosDatabase : RoomDatabase() {
    abstract fun contactoDao(): ContactoDao
    companion object {
        @Volatile
        private var INSTANCE: ContactosDatabase? = null
        fun getDatabase(context: Context): ContactosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactosDatabase::class.java,
                    "contactos_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var database: ContactosDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = ContactosDatabase.getDatabase(this)

        enableEdgeToEdge()
        setContent {
            PM_ListaContactosROOMTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AdministrarContactos(database, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AdministrarContactos(database: ContactosDatabase, modifier: Modifier) {
    val context = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var hayNombre by remember { mutableStateOf(true) }
    var hayCorreo by remember { mutableStateOf(true) }
    var contactos = remember { mutableStateListOf<Contacto>() }

    LaunchedEffect(Unit) {
        database.contactoDao().getTodosContactos().collectLatest { contactosRoom ->
            contactos.clear()
            contactos.addAll(contactosRoom)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre de contacto") },
            singleLine =  true,
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            singleLine =  true,
            modifier = Modifier.fillMaxWidth()
        )

        if (!hayCorreo || !hayNombre) {
            Text(
                text = "Rellena todos los campos!!!",
                color = Color.Red
            )
        }

        Button(
            onClick = {
                hayNombre = nombre.isNotBlank()
                hayCorreo = correo.isNotBlank()

                if (hayNombre && hayCorreo) {
                    val nuevoContacto = Contacto(nombre = nombre, mail = correo)
                    val scope = (context as ComponentActivity).lifecycleScope
                    scope.launch() {
                        database.contactoDao().insertarContacto(nuevoContacto)
                    }
                    correo = ""
                    nombre = ""
                }
            }
        ) {
            Text(
                text = "Agregar"
            )
        }

        LazyColumn {
            itemsIndexed(contactos) { indice, contacto ->
                val scope = rememberCoroutineScope()

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = contacto.nombre)
                    Text(text = contacto.mail)
                    IconButton(
                        onClick = {
                            scope.launch() {
                                database.contactoDao().eliminarContacto(contacto)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Eliminar"
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}