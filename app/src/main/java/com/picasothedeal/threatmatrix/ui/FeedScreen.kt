package com.picasothedeal.threatmatrix.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.picasothedeal.threatmatrix.BuildConfig
import com.picasothedeal.threatmatrix.data.InteractionData
import com.picasothedeal.threatmatrix.data.ThreatLog
import com.picasothedeal.threatmatrix.R
import androidx.compose.material.icons.automirrored.filled.ExitToApp

val CyberDark = Color(0xFF0F0F0F)
val CyberSurface = Color(0xFF0A0A0A)
val CyberMatrix = Color(0xFF5B7A9E)
val CyberGray = Color(0xFF8B949E)
val CyberCritical = Color(0xFFF85149)
val CyberHigh = Color(0xFFF0883E)

class TurnstileJsInterface {
    var onToken: ((String) -> Unit)? = null

    @Suppress("unused")
    @JavascriptInterface
    fun postMessage(token: String) {
        onToken?.invoke(token)
    }
}

@Suppress("SpellCheckingInspection")
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun TurnstileWidget(siteKey: String, onTokenReady: (String) -> Unit) {
    val jsInterface = remember { TurnstileJsInterface() }

    LaunchedEffect(jsInterface) {
        jsInterface.onToken = { token -> onTokenReady(token) }
    }

    AndroidView(
        modifier = Modifier.size(0.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(
                            """
                            if (typeof turnstile === 'undefined') {
                                var script = document.createElement('script');
                                script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
                                script.onload = function() { renderWidget(); };
                                document.head.appendChild(script);
                            } else {
                                renderWidget();
                            }
                            function renderWidget() {
                                var container = document.getElementById('turnstile-container');
                                if (!container) {
                                    container = document.createElement('div');
                                    container.id = 'turnstile-container';
                                    document.body.appendChild(container);
                                }
                                turnstile.render('#turnstile-container', {
                                    sitekey: '$siteKey',
                                    size: 'invisible',
                                    callback: function(token) {
                                        Android.postMessage(token);
                                    }
                                });
                                turnstile.execute('#turnstile-container');
                            }
                            """.trimIndent(), null
                        )
                    }
                }
                addJavascriptInterface(jsInterface, "Android")
                loadDataWithBaseURL("https://threat-matrix.pages.dev", "<html><body style='margin:0;'></body></html>", "text/html", "utf-8", null)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: ThreatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    val uriHandler = LocalUriHandler.current
    val userParameters by viewModel.userParameters.collectAsState()
    val parametersLoaded by viewModel.parametersLoaded.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAuthDialog by remember { mutableStateOf(false) }
    var activeFeedTab by remember { mutableStateOf("cluster") }
    var showTurnstileDialog by remember { mutableStateOf(false) }
    var pendingTurnstileAction by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val isAuthenticated = authState is AuthUiState.Authenticated

    fun requestTokenAndThen(action: (String) -> Unit) {
        pendingTurnstileAction = action
        showTurnstileDialog = true
    }

    val turnstileSiteKey = BuildConfig.TURNSTILE_SITE_KEY.ifEmpty { "1x00000000000000000000AA" }

    LaunchedEffect(isAuthenticated, userParameters, parametersLoaded) {
        if (isAuthenticated && parametersLoaded && userParameters.isEmpty()) {
            showProfileDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "THREAT-MATRIX",
                            color = CyberMatrix,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    uriHandler.openUri("https://github.com/PicasoTheDeal/Threat-Matrix-apk")
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github),
                                contentDescription = "GitHub Repository",
                                tint = Color(0xFFB0B8C1),
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Open Source",
                                color = Color(0xFFB0B8C1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    if (isAuthenticated) {
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configure Parameters",
                                tint = CyberMatrix
                            )
                        }
                    }

                    IconButton(onClick = {
                        if (isAuthenticated) {
                            viewModel.logout()
                        } else {
                            showAuthDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isAuthenticated) Icons.AutoMirrored.Filled.ExitToApp else Icons.Default.AccountCircle,
                            contentDescription = if (isAuthenticated) "Revoke Session" else "Profile Auth",
                            tint = if (isAuthenticated) CyberCritical else CyberGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark)
            )
        },
        containerColor = CyberDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    if (isAuthenticated) {
                        searchQuery = it
                        viewModel.updateSearchQuery(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (isAuthenticated) "Search payloads, CVEs..." else "Pipeline locked...",
                        color = CyberGray
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberGray) },
                singleLine = true,
                enabled = isAuthenticated,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberMatrix,
                    unfocusedBorderColor = CyberSurface,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = CyberGray,
                    disabledBorderColor = CyberSurface,
                    disabledPlaceholderColor = CyberGray
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                OutlinedButton(
                    onClick = { activeFeedTab = "cluster" },
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSurface),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (activeFeedTab == "cluster") Color.White else Color.Black,
                        contentColor = if (activeFeedTab == "cluster") Color.Black else Color.White
                    )
                ) {
                    Text("CLUSTER FEED", fontWeight = FontWeight.Bold)
                }

                if (isAuthenticated) {
                    OutlinedButton(
                        onClick = { activeFeedTab = "selective" },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberSurface),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (activeFeedTab == "selective") Color.White else Color.Black,
                            contentColor = if (activeFeedTab == "selective") Color.Black else Color.White
                        )
                    ) {
                        Text("SELECTIVE FEED", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is ThreatUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyberMatrix)
                    }
                }
                is ThreatUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "CRITICAL_ERROR: ${state.message}", color = CyberCritical, fontWeight = FontWeight.Bold)
                    }
                }
                is ThreatUiState.Success -> {
                    val logsToShow = if (activeFeedTab == "selective" && isAuthenticated) {
                        state.logs.filter { log ->
                            userParameters.any { tag: String ->
                                log.category.contains(tag, ignoreCase = true) ||
                                        log.title.contains(tag, ignoreCase = true) ||
                                        log.excerpt.contains(tag, ignoreCase = true)
                            }
                        }
                    } else {
                        state.logs
                    }

                    if (logsToShow.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "NO LOGS DETECTED IN SIGNATURE SCOPE", color = CyberGray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logsToShow, key = { it.id }) { log ->
                                val interaction = interactions[log.id]
                                ThreatLogCard(
                                    log = log,
                                    interaction = interaction,
                                    isAuthenticated = isAuthenticated,
                                    onLikeToggle = { viewModel.toggleLike(log.id) },
                                    onAddComment = { content ->
                                        requestTokenAndThen { token ->
                                            viewModel.addComment(log.id, content, token)
                                        }
                                    },
                                    onDeleteComment = { commentId ->
                                        viewModel.deleteComment(log.id, commentId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTurnstileDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CyberSurface,
            title = { Text("Verifying...", color = Color.White) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyberMatrix)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Please wait...", color = CyberGray, fontSize = 12.sp)
                    }
                    TurnstileWidget(
                        siteKey = turnstileSiteKey,
                        onTokenReady = { token ->
                            showTurnstileDialog = false
                            pendingTurnstileAction?.invoke(token)
                            pendingTurnstileAction = null
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showTurnstileDialog = false
                    pendingTurnstileAction = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAuthDialog) {
        AuthModalDialog(
            authState = authState,
            onDismiss = { showAuthDialog = false },
            onLogin = { user, pass ->
                requestTokenAndThen { token ->
                    viewModel.login(user, pass, token)
                    showAuthDialog = false
                }
            },
            onSignup = { user, pass ->
                requestTokenAndThen { token ->
                    viewModel.signup(user, pass, token)
                    showAuthDialog = false
                }
            },
            onLogout = { viewModel.logout() }
        )
    }

    if (showProfileDialog) {
        ProfileTagSelectionDialog(
            initialTags = userParameters,
            onDismiss = { showProfileDialog = false },
            onSave = { tags ->
                viewModel.updateProfileTags(tags)
                showProfileDialog = false
            }
        )
    }
}

@Composable
fun ThreatLogCard(
    log: ThreatLog,
    interaction: InteractionData?,
    isAuthenticated: Boolean,
    onLikeToggle: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val liked = interaction?.liked ?: false
    val likesCount = interaction?.likes ?: 0
    val comments = interaction?.comments ?: emptyList()
    val context = LocalContext.current

    val impactColor = when (log.impact?.lowercase()) {
        "critical" -> CyberCritical
        "high" -> CyberHigh
        else -> CyberGray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberSurface, shape = RoundedCornerShape(8.dp))
            .border(1.dp, CyberSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[ ID: ${log.id} ]",
                color = CyberMatrix,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .background(
                        color = impactColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = (log.impact ?: "STANDARD").uppercase(),
                    color = impactColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = log.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                if (log.url.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, log.url.toUri())
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = log.excerpt,
            color = CyberGray,
            fontSize = 13.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis
        )

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                HorizontalDivider(color = CyberDark, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "SOURCE: ${log.source}", color = Color.White, fontSize = 12.sp)
                Text(text = "DATE_LOGGED: ${log.date}", color = Color.White, fontSize = 12.sp)

                if (log.id.startsWith("CVE")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://www.google.com/search?q=${log.id}+exploit".toUri()
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Google",
                                tint = CyberGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://github.com/search?q=${log.id}&type=repositories".toUri()
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "GitHub",
                                tint = CyberGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onLikeToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (liked) CyberCritical else CyberGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = likesCount.toString(), color = CyberGray, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Comments",
                    tint = CyberGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = comments.size.toString(), color = CyberGray, fontSize = 12.sp)
            }
        }

        if (isAuthenticated) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add comment...", color = CyberGray, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberMatrix,
                        unfocusedBorderColor = CyberSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface
                    ),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText.trim())
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (commentText.isNotBlank()) CyberMatrix else CyberGray
                    )
                }
            }

            if (comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    comments.forEach { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberDark, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comment.username,
                                    color = CyberMatrix,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = comment.content,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = { onDeleteComment(comment.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete comment",
                                    tint = CyberCritical,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthModalDialog(
    authState: AuthUiState,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSignup: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = { Text("CORE ACCESS TERMINAL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (authState) {
                    is AuthUiState.Authenticated -> {
                        Text("Active Identity: ${authState.username}", color = CyberMatrix)
                        Text("Security Token successfully verified into volatile memory.", color = CyberGray, fontSize = 12.sp)
                    }
                    else -> {
                        if (authState is AuthUiState.Error) {
                            Text(text = authState.message, color = CyberCritical, fontSize = 12.sp)
                        }
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("User Identifier") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberMatrix, focusedLabelColor = CyberMatrix)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Cipher Token") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberMatrix, focusedLabelColor = CyberMatrix)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (authState is AuthUiState.Authenticated) {
                Button(onClick = { onLogout(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = CyberCritical)) {
                    Text("REVOKE SESSION")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onLogin(username, password) }) {
                        Text("AUTHENTICATE", color = CyberMatrix)
                    }
                    TextButton(onClick = { onSignup(username, password) }) {
                        Text("REGISTER_NEW", color = Color.White)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("SpellCheckingInspection")
@Composable
fun ProfileTagSelectionDialog(
    initialTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val allTags = remember {
        mapOf(
            "Web Frameworks & Libs" to listOf("React", "Vue", "Angular", "Svelte", "Laravel", "Django", "Flask", "Spring", "Next.js", "Nuxt", "Vite", "Express", "Ruby on Rails", "ASP.NET", "jQuery", "NPM"),
            "Attack Vectors & Threats" to listOf("Zero-Day", "Malware", "Phishing", "Ransomware", "RAT", "DDoS", "SQL Injection", "XSS", "CSRF", "Buffer Overflow", "Man-in-the-Middle", "Privilege Escalation", "Supply Chain Attack"),
            "Operating Systems" to listOf("Linux", "Windows", "macOS", "Android", "iOS", "FreeBSD", "Ubuntu", "Debian", "Arch Linux", "Kali Linux", "RedHat", "CentOS"),
            "Networking & Hardware" to listOf("Cisco", "Juniper", "Fortinet", "Palo Alto", "Ubiquiti", "NVIDIA", "Intel", "AMD", "Broadcom")
        )
    }

    var selectedTags by remember { mutableStateOf(initialTags.map { it.uppercase() }.toSet()) }
    var configSearch by remember { mutableStateOf("") }

    val allFlatTags = remember(allTags) { allTags.values.flatten().map { it.uppercase() } }

    val isSearchCustom = configSearch.isNotBlank() && !allFlatTags.contains(configSearch.trim().uppercase())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Column {
                Text("INITIALIZE TRACKING PROFILE", color = CyberMatrix, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Select matrix nodes to isolate parameters across the tracking stream.", color = CyberGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                OutlinedTextField(
                    value = configSearch,
                    onValueChange = { configSearch = it },
                    placeholder = { Text("> Search library tags or define custom parameters...", color = CyberGray, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0D0D0D),
                        unfocusedContainerColor = Color(0xFF0D0D0D),
                        focusedBorderColor = CyberMatrix,
                        unfocusedBorderColor = Color(0xFF1C2333)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp)
                )

                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    if (isSearchCustom) {
                        val customTag = configSearch.trim().uppercase()
                        val isCustomSelected = selectedTags.contains(customTag)

                        Text("[ Custom Parameter Matrix ]", color = CyberMatrix, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

                        Box(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .background(if (isCustomSelected) Color.White else Color(0xFF222222), RoundedCornerShape(4.dp))
                                .border(1.dp, CyberMatrix, RoundedCornerShape(4.dp))
                                .clickable {
                                    selectedTags = if (isCustomSelected) selectedTags - customTag else selectedTags + customTag
                                    configSearch = ""
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+ ADD \"$customTag\"", color = if (isCustomSelected) CyberSurface else CyberMatrix, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    allTags.forEach { (category, tags) ->
                        val visibleTags = tags.filter { it.contains(configSearch, ignoreCase = true) }

                        if (visibleTags.isNotEmpty()) {
                            Text("[ $category ]", color = CyberGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp, top = if (category == allTags.keys.first()) 0.dp else 16.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                visibleTags.forEach { tag ->
                                    val formattedTag = tag.uppercase()
                                    val isChosen = selectedTags.contains(formattedTag)

                                    Box(
                                        modifier = Modifier
                                            .background(if (isChosen) Color.White else Color(0xFF0D0D0D), RoundedCornerShape(4.dp))
                                            .border(1.dp, if (isChosen) Color.White else Color(0xFF1C2333), RoundedCornerShape(4.dp))
                                            .clickable {
                                                selectedTags = if (isChosen) selectedTags - formattedTag else selectedTags + formattedTag
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(tag, color = if (isChosen) CyberSurface else Color.White, fontSize = 12.sp, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTags.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF1C2333), thickness = 1.dp)
                        Text("[ Target Profile Definitions ]", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White, RoundedCornerShape(4.dp))
                                        .clickable { selectedTags = selectedTags - tag }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("$tag ✕", color = CyberSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NODES PROFILED: ${selectedTags.size}", color = CyberGray, fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = CyberGray)
                    }
                    Button(
                        onClick = { onSave(selectedTags.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberMatrix),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("DONE", color = Color.White)
                    }
                }
            }
        },
        dismissButton = null
    )
}