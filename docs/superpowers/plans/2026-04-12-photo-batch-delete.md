# Photo Batch Delete — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add multi-select batch deletion to AllPhotosScreen so users can long-press to enter selection mode, pick multiple photos, and delete them all at once.

**Architecture:** Follows the existing multi-select pattern from ProjectListScreen/ProjectListViewModel. Selection state lives in LibraryViewModel (already scoped to Library tab). AllPhotosScreen switches between normal top bar and selection top bar based on mode. A bottom bar with a delete button appears when photos are selected.

**Tech Stack:** Jetpack Compose, Room DAO, Hilt ViewModel, StateFlow, Material3

---

### Task 1: Add batch delete to DAO

**Files:**
- Modify: `app/src/main/java/com/finnvek/knittools/data/local/ProgressPhotoDao.kt:39-40`

- [ ] **Step 1: Add deleteByIds query to ProgressPhotoDao**

Add a new query below the existing `delete(id)` at line 40:

```kotlin
@Query("DELETE FROM progress_photos WHERE id IN (:ids)")
suspend fun deleteByIds(ids: List<Long>)

@Query("SELECT * FROM progress_photos WHERE id IN (:ids)")
suspend fun getByIds(ids: List<Long>): List<ProgressPhotoEntity>
```

The full file becomes:

```kotlin
package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressPhotoDao {
    @Query("SELECT * FROM progress_photos WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getPhotosForProject(projectId: Long): Flow<List<ProgressPhotoEntity>>

    @Query(
        "SELECT * FROM progress_photos WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT :limit",
    )
    fun getLatestPhotos(
        projectId: Long,
        limit: Int = 5,
    ): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT COUNT(*) FROM progress_photos WHERE projectId = :projectId")
    fun getPhotoCount(projectId: Long): Flow<Int>

    @Query("SELECT * FROM progress_photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT COUNT(*) FROM progress_photos")
    fun getAllPhotoCount(): Flow<Int>

    @Insert
    suspend fun insert(photo: ProgressPhotoEntity): Long

    @Query("UPDATE progress_photos SET note = :note WHERE id = :id")
    suspend fun updateNote(
        id: Long,
        note: String?,
    )

    @Query("DELETE FROM progress_photos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM progress_photos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM progress_photos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ProgressPhotoEntity>
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/finnvek/knittools/data/local/ProgressPhotoDao.kt
git commit -m "Lisää batch-poisto- ja haku-queryt ProgressPhotoDao:lle"
```

---

### Task 2: Add batch delete to repository

**Files:**
- Modify: `app/src/main/java/com/finnvek/knittools/repository/ProgressPhotoRepository.kt:59-64`

- [ ] **Step 1: Add deletePhotos method to ProgressPhotoRepository**

Add below the existing `deletePhoto` method (line 64):

```kotlin
suspend fun deletePhotos(ids: List<Long>) {
    val photos = dao.getByIds(ids)
    withContext(Dispatchers.IO) {
        photos.forEach { storage.deletePhoto(it.photoUri) }
    }
    dao.deleteByIds(ids)
}
```

The full file becomes:

```kotlin
package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressPhotoRepository
    @Inject
    constructor(
        private val dao: ProgressPhotoDao,
        private val storage: ProgressPhotoStorage,
        @param:ApplicationContext private val context: Context,
    ) {
        fun getAllPhotos(): Flow<List<ProgressPhotoEntity>> = dao.getAllPhotos()

        fun getAllPhotoCount(): Flow<Int> = dao.getAllPhotoCount()

        fun getPhotosForProject(projectId: Long): Flow<List<ProgressPhotoEntity>> = dao.getPhotosForProject(projectId)

        fun getLatestPhotos(projectId: Long): Flow<List<ProgressPhotoEntity>> = dao.getLatestPhotos(projectId)

        fun getPhotoCount(projectId: Long): Flow<Int> = dao.getPhotoCount(projectId)

        suspend fun savePhoto(
            projectId: Long,
            sourceUri: Uri,
            rowNumber: Int,
            note: String? = null,
        ): Long =
            withContext(Dispatchers.IO) {
                val (file, _) = storage.createPhotoFile(context, projectId)
                storage.compressAndSave(context, sourceUri, file)
                dao.insert(
                    ProgressPhotoEntity(
                        projectId = projectId,
                        photoUri = Uri.fromFile(file).toString(),
                        rowNumber = rowNumber,
                        note = note?.take(100),
                    ),
                )
            }

        suspend fun updatePhotoNote(
            id: Long,
            note: String?,
        ) {
            dao.updateNote(id, note?.take(100)?.ifBlank { null })
        }

        suspend fun deletePhoto(photo: ProgressPhotoEntity) {
            withContext(Dispatchers.IO) {
                storage.deletePhoto(photo.photoUri)
            }
            dao.delete(photo.id)
        }

        suspend fun deletePhotos(ids: List<Long>) {
            val photos = dao.getByIds(ids)
            withContext(Dispatchers.IO) {
                photos.forEach { storage.deletePhoto(it.photoUri) }
            }
            dao.deleteByIds(ids)
        }

        fun deleteAllPhotosForProject(projectId: Long) {
            storage.deleteProjectPhotos(context, projectId)
        }
    }
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/finnvek/knittools/repository/ProgressPhotoRepository.kt
git commit -m "Lisää deletePhotos batch-metodi ProgressPhotoRepositoryyn"
```

---

### Task 3: Add multi-select state to LibraryViewModel

**Files:**
- Modify: `app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryViewModel.kt`

- [ ] **Step 1: Add selection state and methods to LibraryViewModel**

Add after the existing `deletePhoto` method (line 51), before the closing brace:

```kotlin
// === Multi-select (AllPhotosScreen) ===

private val _isPhotoSelectMode = MutableStateFlow(false)
val isPhotoSelectMode: StateFlow<Boolean> = _isPhotoSelectMode.asStateFlow()

private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

fun enterPhotoSelectMode(initialPhotoId: Long) {
    _isPhotoSelectMode.value = true
    _selectedPhotoIds.value = setOf(initialPhotoId)
}

fun exitPhotoSelectMode() {
    _isPhotoSelectMode.value = false
    _selectedPhotoIds.value = emptySet()
}

fun togglePhotoSelection(id: Long) {
    _selectedPhotoIds.update { current ->
        val next = if (id in current) current - id else current + id
        if (next.isEmpty()) {
            _isPhotoSelectMode.value = false
        }
        next
    }
}

fun selectAllPhotos(visibleIds: List<Long>) {
    _selectedPhotoIds.value = visibleIds.toSet()
}

fun deleteSelectedPhotos() {
    viewModelScope.launch {
        val ids = _selectedPhotoIds.value.toList()
        progressPhotoRepository.deletePhotos(ids)
        exitPhotoSelectMode()
    }
}
```

The full file becomes:

```kotlin
package com.finnvek.knittools.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        savedPatternRepository: SavedPatternRepository,
        yarnCardRepository: YarnCardRepository,
        private val progressPhotoRepository: ProgressPhotoRepository,
        counterRepository: CounterRepository,
    ) : ViewModel() {
        // Countit Library-hubille
        val savedPatternCount: Flow<Int> = savedPatternRepository.getCount()
        val yarnCardCount: Flow<Int> = yarnCardRepository.getCardCount()
        val photoCount: Flow<Int> = progressPhotoRepository.getAllPhotoCount()

        // Listat alanäytöille
        val savedPatterns: Flow<List<SavedPatternEntity>> = savedPatternRepository.getAll()
        val yarnCards: Flow<List<YarnCardEntity>> = yarnCardRepository.getAllCards()
        val allPhotos: Flow<List<ProgressPhotoEntity>> = progressPhotoRepository.getAllPhotos()
        val allProjects: Flow<List<CounterProjectEntity>> = counterRepository.getAllProjects()
        val activeProjectNames: Flow<Map<Long, String>> =
            counterRepository
                .getAllProjects()
                .map { projects ->
                    projects
                        .filterNot { it.isCompleted }
                        .associate { it.id to it.name }
                }

        fun deletePhoto(photo: ProgressPhotoEntity) {
            viewModelScope.launch {
                progressPhotoRepository.deletePhoto(photo)
            }
        }

        // === Multi-select (AllPhotosScreen) ===

        private val _isPhotoSelectMode = MutableStateFlow(false)
        val isPhotoSelectMode: StateFlow<Boolean> = _isPhotoSelectMode.asStateFlow()

        private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

        fun enterPhotoSelectMode(initialPhotoId: Long) {
            _isPhotoSelectMode.value = true
            _selectedPhotoIds.value = setOf(initialPhotoId)
        }

        fun exitPhotoSelectMode() {
            _isPhotoSelectMode.value = false
            _selectedPhotoIds.value = emptySet()
        }

        fun togglePhotoSelection(id: Long) {
            _selectedPhotoIds.update { current ->
                val next = if (id in current) current - id else current + id
                if (next.isEmpty()) {
                    _isPhotoSelectMode.value = false
                }
                next
            }
        }

        fun selectAllPhotos(visibleIds: List<Long>) {
            _selectedPhotoIds.value = visibleIds.toSet()
        }

        fun deleteSelectedPhotos() {
            viewModelScope.launch {
                val ids = _selectedPhotoIds.value.toList()
                progressPhotoRepository.deletePhotos(ids)
                exitPhotoSelectMode()
            }
        }
    }
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryViewModel.kt
git commit -m "Lisää multi-select-tila ja batch-poisto LibraryViewModeliin"
```

---

### Task 4: Add string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add selection-related strings**

The strings `n_selected` and `select_all` already exist (lines 89-90). Add a new batch delete confirmation string near the existing `delete_photo_confirm` (line 494):

```xml
<string name="delete_photos_confirm">Delete %d photos?</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "Lisää batch-poiston string-resurssit"
```

---

### Task 5: Rewrite AllPhotosScreen with multi-select support

**Files:**
- Modify: `app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt`

This is the main UI task. The screen switches between two modes:

**Normal mode:** Current behavior — click opens PhotoViewer.
**Selection mode:** Long-press activates, click toggles selection, custom top bar with "N selected" + X + Select All, bottom bar with Delete button.

- [ ] **Step 1: Update AllPhotosScreen signature and state**

Change the function signature to accept selection state and callbacks:

```kotlin
@Composable
fun AllPhotosScreen(
    photos: List<ProgressPhotoEntity>,
    projects: List<CounterProjectEntity>,
    isSelectMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onDeletePhoto: (ProgressPhotoEntity) -> Unit,
    onEnterSelectMode: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onDeleteSelected: () -> Unit,
    onExitSelectMode: () -> Unit,
    onBack: () -> Unit,
)
```

- [ ] **Step 2: Write the full AllPhotosScreen composable**

Replace the entire file content:

```kotlin
package com.finnvek.knittools.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.screens.counter.PhotoViewer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPhotosScreen(
    photos: List<ProgressPhotoEntity>,
    projects: List<CounterProjectEntity>,
    isSelectMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onDeletePhoto: (ProgressPhotoEntity) -> Unit,
    onEnterSelectMode: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onDeleteSelected: () -> Unit,
    onExitSelectMode: () -> Unit,
    onBack: () -> Unit,
) {
    var selectedProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewingPhoto by remember { mutableStateOf<ProgressPhotoEntity?>(null) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val projectMap = projects.associateBy { it.id }
    val projectsWithPhotos = photos.map { it.projectId }.distinct()
    val filteredPhotos =
        if (selectedProjectId != null) {
            photos.filter { it.projectId == selectedProjectId }
        } else {
            photos
        }

    // Back handler poistuu select-modesta
    BackHandler(enabled = isSelectMode) {
        onExitSelectMode()
    }

    // PhotoViewer (vain normaalitilassa)
    viewingPhoto?.let { photo ->
        PhotoViewer(
            photo = photo,
            onDismiss = { viewingPhoto = null },
            onDelete = {
                onDeletePhoto(it)
                viewingPhoto = null
            },
        )
    }

    // Batch-poiston vahvistusdialogi
    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_photo),
            message = stringResource(R.string.delete_photos_confirm, selectedPhotoIds.size),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                onDeleteSelected()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.n_selected, selectedPhotoIds.size),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onExitSelectMode) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { onSelectAll(filteredPhotos.map { it.id }) }) {
                            Text(stringResource(R.string.select_all))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.all_photos_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectMode && selectedPhotoIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_all_photos),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
                // Filter chips
                if (!isSelectMode) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = selectedProjectId == null,
                                onClick = { selectedProjectId = null },
                                label = { Text(stringResource(R.string.filter_all)) },
                            )
                        }
                        items(projectsWithPhotos) { projectId ->
                            val name = projectMap[projectId]?.name ?: "Project $projectId"
                            FilterChip(
                                selected = selectedProjectId == projectId,
                                onClick = { selectedProjectId = projectId },
                                label = {
                                    Text(
                                        text = name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Photo grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredPhotos, key = { it.id }) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            projectName = projectMap[photo.projectId]?.name,
                            isSelectMode = isSelectMode,
                            isSelected = photo.id in selectedPhotoIds,
                            onClick = {
                                if (isSelectMode) {
                                    onToggleSelection(photo.id)
                                } else {
                                    viewingPhoto = photo
                                }
                            },
                            onLongClick = {
                                if (!isSelectMode) {
                                    onEnterSelectMode(photo.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: ProgressPhotoEntity,
    projectName: String?,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())

    Surface(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .then(
                if (isSelected) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        MaterialTheme.shapes.medium,
                    )
                } else {
                    Modifier
                },
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column {
            Box {
                AsyncImage(
                    model = photo.photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
                // Valintaindikaattori
                if (isSelectMode) {
                    Icon(
                        imageVector = if (isSelected) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.Circle
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        },
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                if (projectName != null) {
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = photo.note ?: "Row ${photo.rowNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dateFormat.format(Date(photo.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: FAIL — the call site in navigation hasn't been updated yet. That's next.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt
git commit -m "Lisää multi-select UI AllPhotosScreeniin (long-press, top bar, bottom bar)"
```

---

### Task 6: Update navigation call site

**Files:**
- The file that creates the `AllPhotosScreen` composable in the navigation graph. Find it with:

```bash
grep -rn "AllPhotosScreen(" app/src/main/java/ --include="*.kt" | grep -v "^app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt"
```

This will show the navigation file where `AllPhotosScreen` is called. The call site needs to be updated to pass the new multi-select parameters from `LibraryViewModel`.

- [ ] **Step 1: Find the call site**

Run the grep above and read the file. Look for where `AllPhotosScreen(` is called and `LibraryViewModel` is available.

- [ ] **Step 2: Update the call site**

At the call site, the `LibraryViewModel` should already be accessible (it's scoped to the Library tab). Add the new state collections and callbacks:

```kotlin
val isPhotoSelectMode by libraryViewModel.isPhotoSelectMode.collectAsStateWithLifecycle()
val selectedPhotoIds by libraryViewModel.selectedPhotoIds.collectAsStateWithLifecycle()
```

Then update the `AllPhotosScreen` call to include all new parameters:

```kotlin
AllPhotosScreen(
    photos = allPhotos,
    projects = allProjects,
    isSelectMode = isPhotoSelectMode,
    selectedPhotoIds = selectedPhotoIds,
    onDeletePhoto = { libraryViewModel.deletePhoto(it) },
    onEnterSelectMode = { libraryViewModel.enterPhotoSelectMode(it) },
    onToggleSelection = { libraryViewModel.togglePhotoSelection(it) },
    onSelectAll = { libraryViewModel.selectAllPhotos(it) },
    onDeleteSelected = { libraryViewModel.deleteSelectedPhotos() },
    onExitSelectMode = { libraryViewModel.exitPhotoSelectMode() },
    onBack = { navController.popBackStack() },
)
```

The exact parameter names and how `allPhotos`/`allProjects` are collected depends on the existing code at the call site — follow the same pattern that's already there.

- [ ] **Step 3: Verify full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add <navigation-file-path>
git commit -m "Kytke AllPhotosScreen multi-select navigaatioon"
```

---

### Task 7: Manual testing

- [ ] **Step 1: Test normal mode is unchanged**

1. Open Library > Photos
2. Tap a photo — it should open PhotoViewer
3. Delete a single photo from PhotoViewer — works as before
4. Filter chips should work normally

- [ ] **Step 2: Test long-press activation**

1. Long-press a photo — selection mode activates
2. Top bar changes: X icon, "1 selected", "Select All" button
3. The long-pressed photo shows a check circle overlay
4. Filter chips are hidden

- [ ] **Step 3: Test selection toggling**

1. Tap other photos — they get selected/deselected
2. Count in top bar updates
3. If all photos are deselected, selection mode exits automatically

- [ ] **Step 4: Test Select All**

1. Enter selection mode, tap "Select All"
2. All visible (filtered) photos become selected
3. Count matches total visible photos

- [ ] **Step 5: Test batch delete**

1. Select multiple photos, tap Delete button in bottom bar
2. Confirmation dialog shows "Delete N photos?"
3. Confirm — all selected photos are deleted, mode exits
4. Cancel — nothing deleted, stays in selection mode

- [ ] **Step 6: Test back button**

1. Enter selection mode, press system back
2. Selection mode exits, returns to normal mode (doesn't navigate back)

- [ ] **Step 7: Commit if any fixes were needed**

```bash
git add -u
git commit -m "Korjaa multi-select testissä löydetyt ongelmat"
```
