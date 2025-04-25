// ────────────────────────────────────────────────────────────────────────────────
// RegisterScreenClass.kt          ⟨FULL FILE – paste as-is⟩
// ────────────────────────────────────────────────────────────────────────────────
package net.swofty.catchngo.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.swofty.catchngo.R
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.models.AuthViewModel
import net.swofty.catchngo.models.ImageViewModel
import net.swofty.catchngo.models.QuestionsViewModel
import java.io.ByteArrayOutputStream

class RegisterScreenClass {

    /* -------------------------------------------------------------------- */
    /*  Colours & fonts                                                     */
    /* -------------------------------------------------------------------- */
    private val darkBackground = Color(0xFF15202B)
    private val darkSurface    = Color(0xFF1E2732)
    private val accentBlue     = Color(0xFF1DA1F2)
    private val textWhite      = Color(0xFFE7E9EA)
    private val textSecondary  = Color(0xFF8899A6)

    private val poppinsFamily = FontFamily(
        Font(R.font.poppins_regular,  FontWeight.Normal),
        Font(R.font.poppins_medium,   FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold,     FontWeight.Bold)
    )

    /* -------------------------------------------------------------------- */
    /*  Composable entry-point                                              */
    /* -------------------------------------------------------------------- */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RegisterScreen(
        onRegisterSuccess : () -> Unit,
        onNavigateToLogin : () -> Unit,
        authViewModel     : AuthViewModel,
        questionsViewModel: QuestionsViewModel,
        imageViewModel    : ImageViewModel
    ) {
        /* ------------ VM state ---------------- */
        val registerState  by authViewModel.registerState.observeAsState()
        val loginState  by authViewModel.loginState.observeAsState()
        val questionsState by questionsViewModel.state.observeAsState(QuestionsViewModel.State.Loading)
        val uploadState    by imageViewModel.uploadState.observeAsState(ImageViewModel.UploadState.Initial)

        /* ------------ Local form state -------- */
        var username        by remember { mutableStateOf("") }
        var password        by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        val questionAnswers = remember { mutableStateMapOf<Int, String>() }

        /* ------------ Internal flow flags ----- */
        var loginTriggered  by remember { mutableStateOf(false) }
        var uploadTriggered by remember { mutableStateOf(false) }

        /* ------------ Picture state ----------- */
        var picBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var picBase64 by remember { mutableStateOf<String?>(null) }

        /* ------------ Validation flags -------- */
        var attemptedSubmit        by remember { mutableStateOf(false) }
        var usernameError          by remember { mutableStateOf(false) }
        var passwordError          by remember { mutableStateOf(false) }
        var confirmPasswordError   by remember { mutableStateOf(false) }
        var questionsError         by remember { mutableStateOf(false) }
        var pictureError           by remember { mutableStateOf(false) }
        var errorBannerMessage     by remember { mutableStateOf<String?>(null) }

        /* ------------ One-shot helpers -------- */
        LaunchedEffect(Unit) { questionsViewModel.refresh() }

        /* 1️⃣ register done → trigger login */
        LaunchedEffect(registerState) {
            if (registerState is AuthViewModel.RegisterState.Success && !loginTriggered) {
                loginTriggered = true              // guard
                authViewModel.login(username, password)
            }
        }

        /* 2️⃣ login done → upload picture */
        LaunchedEffect(loginState) {
            if (loginState is AuthViewModel.LoginState.Success &&
                picBase64 != null &&
                !uploadTriggered) {
                uploadTriggered = true             // guard
                imageViewModel.uploadPicture(picBase64!!)
            }
        }

        /* 3️⃣ upload done → final callback */
        LaunchedEffect(uploadState) {
            if (uploadState is ImageViewModel.UploadState.Success) {
                onRegisterSuccess()
            }
        }

        /* ------------ Image picker launcher --- */
        val context = LocalContext.current
        val pickImageLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            picBitmap = loadBitmap(context, uri)
            picBase64 = picBitmap?.toBase64()
        }

        /* ---------------------------------------------------------------- */
        /*  Layout                                                          */
        /* ---------------------------------------------------------------- */
        Box(
            Modifier
                .fillMaxSize()
                .background(darkBackground)
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /* ---------- Title ---------- */
                item {
                    Text(
                        "Catch N Go",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        color      = textWhite,
                        fontSize   = 32.sp,
                        textAlign  = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create your account",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Medium,
                        color      = textSecondary,
                        fontSize   = 16.sp,
                        textAlign  = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                }

                /* ---------- Profile picture (square) ---------- */
                item {
                    ProfilePicturePicker(
                        bitmap        = picBitmap,
                        onPickClicked = { pickImageLauncher.launch("image/*") },
                        error         = pictureError
                    )
                    Spacer(Modifier.height(24.dp))
                }

                /* ---------- Username ---------- */
                item {
                    FieldUsername(
                        value           = username,
                        onChange        = { username = it },
                        attemptedSubmit = attemptedSubmit,
                        showError       = usernameError
                    )
                }

                /* ---------- Password ---------- */
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    FieldPassword(
                        value           = password,
                        onChange        = { password = it },
                        attemptedSubmit = attemptedSubmit,
                        showError       = passwordError,
                        label           = "Password"
                    )
                }

                /* ---------- Confirm password ---------- */
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    FieldPassword(
                        value           = confirmPassword,
                        onChange        = { confirmPassword = it },
                        attemptedSubmit = attemptedSubmit,
                        showError       = confirmPasswordError,
                        label           = "Confirm Password"
                    )
                }

                /* ---------- Personality questions ---------- */
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Personality Questions",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        color      = textWhite,
                        fontSize   = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }

                when (val qs = questionsState) {
                    is QuestionsViewModel.State.Success -> {
                        items(qs.questions) { q ->
                            val qErr = attemptedSubmit && questionAnswers[q.id].isNullOrBlank()
                            QuestionItem(
                                question       = q,
                                answer         = questionAnswers[q.id] ?: "",
                                onAnswerChange = { ans -> questionAnswers[q.id] = ans },
                                showError      = qErr
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    is QuestionsViewModel.State.Loading -> item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = accentBlue) }
                    }
                    is QuestionsViewModel.State.Error -> item {
                        Text(
                            "Error loading questions: ${qs.message}",
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.error,
                            modifier   = Modifier.padding(16.dp)
                        )
                        Box(Modifier.fillMaxWidth(), Alignment.Center) {
                            Button(
                                onClick = questionsViewModel::refresh,
                                colors  = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                shape   = RoundedCornerShape(50.dp)
                            ) { Text("Retry", fontFamily = poppinsFamily) }
                        }
                    }
                    else -> {}
                }

                /* ---------- Error banner / Sign-up button ---------- */
                item {
                    errorBannerMessage?.let {
                        Text(
                            it,
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.error,
                            modifier   = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val busy = registerState is AuthViewModel.RegisterState.Success ||
                            loginState    is AuthViewModel.LoginState.Success ||
                            uploadState   is ImageViewModel.UploadState.Loading

                    Button(
                        onClick = {
                            attemptedSubmit = true
                            val errors = validate(
                                username, password, confirmPassword,
                                questionAnswers, picBase64
                            )

                            usernameError        = errors.usernameMissing
                            passwordError        = errors.passwordMissing
                            confirmPasswordError = errors.confirmPasswordMissing || errors.passwordsMismatch
                            questionsError       = errors.questionsIncomplete
                            pictureError         = errors.pictureMissing

                            errorBannerMessage = when {
                                errors.pictureMissing      -> "Please choose a profile picture"
                                errors.usernameMissing ||
                                        errors.passwordMissing ||
                                        errors.confirmPasswordMissing ||
                                        errors.questionsIncomplete   -> "Please fill in all required fields"
                                errors.passwordsMismatch   -> "Passwords do not match"
                                else -> null
                            }

                            if (errors.clean()) {
                                loginTriggered   =  false
                                uploadTriggered  =  false

                                authViewModel.register(
                                    username,
                                    password,
                                    questionAnswers.map { (id, ans) ->
                                        ApiModels.QuestionAnswer(id, ans)
                                    }
                                )
                            }
                        },
                        enabled = !busy,
                        shape   = RoundedCornerShape(50.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor         = accentBlue,
                            contentColor           = Color.White,
                            disabledContainerColor = accentBlue.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(
                                1.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f)
                                    ),
                                    center = Offset.Zero
                                ),
                                RoundedCornerShape(50.dp)
                            )
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(24.dp), Color.White)
                        } else {
                            Text(
                                "Sign up",
                                fontFamily = poppinsFamily,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    TextButton(onNavigateToLogin) {
                        Text(
                            "Already have an account? Sign in",
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.Medium,
                            color      = accentBlue
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Profile-picture picker (square)                                     */
    /* -------------------------------------------------------------------- */
    @Composable
    private fun ProfilePicturePicker(
        bitmap       : Bitmap?,
        onPickClicked: () -> Unit,
        error        : Boolean
    ) {
        val borderMod = if (error) Modifier.border(2.dp, Color.Red, RoundedCornerShape(12.dp))
        else Modifier

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(120.dp)                       // square
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .then(borderMod)
                    .background(darkSurface),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize())
                } else {
                    IconButton(onPickClicked) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Pick photo",
                            tint   = accentBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            if (bitmap == null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Add profile picture",
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Medium,
                    color      = textSecondary,
                    fontSize   = 14.sp
                )
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Username & Password fields                                          */
    /* -------------------------------------------------------------------- */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FieldUsername(
        value           : String,
        onChange        : (String) -> Unit,
        attemptedSubmit : Boolean,
        showError       : Boolean
    ) {
        val src  = remember { MutableInteractionSource() }
        val focus by src.collectIsFocusedAsState()

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .borderForField(focus, showError)
        ) {
            TextField(
                value = value,
                onValueChange = onChange,
                interactionSource = src,
                placeholder = {
                    Text("Username", fontFamily = poppinsFamily, color = textSecondary)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person, null,
                        tint = if (focus) accentBlue else textSecondary
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor         = darkSurface,
                    cursorColor            = accentBlue,
                    focusedTextColor       = textWhite,
                    unfocusedTextColor     = textWhite,
                    focusedIndicatorColor  = Color.Transparent,
                    unfocusedIndicatorColor= Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = poppinsFamily),
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FieldPassword(
        value           : String,
        onChange        : (String) -> Unit,
        attemptedSubmit : Boolean,
        showError       : Boolean,
        label           : String
    ) {
        val src  = remember { MutableInteractionSource() }
        val focus by src.collectIsFocusedAsState()

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .borderForField(focus, showError)
        ) {
            TextField(
                value = value,
                onValueChange = onChange,
                interactionSource = src,
                placeholder = {
                    Text(label, fontFamily = poppinsFamily, color = textSecondary)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock, null,
                        tint = if (focus) accentBlue else textSecondary
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor         = darkSurface,
                    cursorColor            = accentBlue,
                    focusedTextColor       = textWhite,
                    unfocusedTextColor     = textWhite,
                    focusedIndicatorColor  = Color.Transparent,
                    unfocusedIndicatorColor= Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = poppinsFamily),
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Question item                                                       */
    /* -------------------------------------------------------------------- */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun QuestionItem(
        question       : ApiModels.Question,
        answer         : String,
        onAnswerChange : (String) -> Unit,
        showError      : Boolean
    ) {
        val src  = remember { MutableInteractionSource() }
        val focus by src.collectIsFocusedAsState()

        Column {
            Text(
                question.questionText,
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.Medium,
                color      = textWhite,
                fontSize   = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .borderForField(focus, showError)
            ) {
                TextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    interactionSource = src,
                    placeholder = {
                        Text("Your answer", fontFamily = poppinsFamily, color = textSecondary)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AddCircle, null,
                            tint = if (focus) accentBlue else textSecondary
                        )
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor         = darkSurface,
                        cursorColor            = accentBlue,
                        focusedTextColor       = textWhite,
                        unfocusedTextColor     = textWhite,
                        focusedIndicatorColor  = Color.Transparent,
                        unfocusedIndicatorColor= Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = poppinsFamily),
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Validation helpers                                                  */
    /* -------------------------------------------------------------------- */
    private data class ValidationErrors(
        val usernameMissing       : Boolean,
        val passwordMissing       : Boolean,
        val confirmPasswordMissing: Boolean,
        val passwordsMismatch     : Boolean,
        val questionsIncomplete   : Boolean,
        val pictureMissing        : Boolean
    ) {
        fun clean() = !(usernameMissing || passwordMissing || confirmPasswordMissing ||
                passwordsMismatch || questionsIncomplete || pictureMissing)
    }

    private fun validate(
        username       : String,
        password       : String,
        confirmPassword: String,
        qAns           : Map<Int, String>,
        pictureB64     : String?
    ): ValidationErrors =
        ValidationErrors(
            usernameMissing        = username.isBlank(),
            passwordMissing        = password.isBlank(),
            confirmPasswordMissing = confirmPassword.isBlank(),
            passwordsMismatch      = password != confirmPassword,
            questionsIncomplete    = qAns.any { it.value.isBlank() },
            pictureMissing         = pictureB64 == null
        )

    /* -------------------------------------------------------------------- */
    /*  Styling helpers                                                     */
    /* -------------------------------------------------------------------- */
    private fun Modifier.borderForField(focus: Boolean, error: Boolean) = when {
        error -> border(2.dp, Color.Red, RoundedCornerShape(12.dp))
        focus -> border(
            2.dp,
            Brush.sweepGradient(
                listOf(
                    accentBlue.copy(alpha = 0.2f),
                    accentBlue.copy(alpha = 0.5f),
                    accentBlue.copy(alpha = 0.8f),
                    accentBlue,
                    accentBlue.copy(alpha = 0.8f),
                    accentBlue.copy(alpha = 0.5f),
                    accentBlue.copy(alpha = 0.2f)
                ),
                center = Offset.Zero
            ),
            RoundedCornerShape(12.dp)
        )
        else -> this
    }

    /* -------------------------------------------------------------------- */
    /*  Bitmap / Base-64 helpers                                            */
    /* -------------------------------------------------------------------- */
    private fun loadBitmap(ctx: android.content.Context, uri: Uri): Bitmap? =
        runCatching {
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
            }
        }.getOrNull()

    private fun Bitmap.toBase64(): String {
        val bos = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, bos)
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }
}
