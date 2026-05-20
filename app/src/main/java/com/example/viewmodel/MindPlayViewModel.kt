package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface StoryState {
    object Idle : StoryState
    object Loading : StoryState
    data class Active(val storyText: String, val choices: List<String>, val history: String) : StoryState
    data class Conclusion(val storyText: String) : StoryState
    data class Error(val message: String) : StoryState
}

class MindPlayViewModel(private val repository: MindPlayRepository) : ViewModel() {

    val streakState: StateFlow<StreakState?> = repository.streakState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mindfulLogs: StateFlow<List<MindfulLog>> = repository.mindfulLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<Badge>> = repository.badges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntry>> = repository.leaderboard
        .onEach {
            if (it.isEmpty()) {
                repository.prepopulateLeaderboardIfEmpty()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _storyState = MutableStateFlow<StoryState>(StoryState.Idle)
    val storyState: StateFlow<StoryState> = _storyState.asStateFlow()

    private val _aiReflectionFeedback = MutableStateFlow<String?>(null)
    val aiReflectionFeedback: StateFlow<String?> = _aiReflectionFeedback.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Interactive Short Stories Library (Offline Fallback)
    private val offlineStories = mapOf(
        "Serene Island" to listOf(
            OfflineStoryNode(
                id = 1,
                text = "You awaken on a soft bed of silver grass on a floating island. Below, a gentle sea of white clouds rolls infinitely. A small amber bird sits on your shoulder, singing a single calming note that aligns with your heartbeat. Before you lie two paths.",
                choiceA = "Follow the path of glowing cyan stones leading up to a mountain spire",
                choiceB = "Step onto the suspension bridge woven from vines disappearing into the deep woods"
            ),
            OfflineStoryNode(
                id = 2, // From choice A
                text = "You ascend the quiet spires of cyan stone. With each step, the air grows crisp and light. At the peak, you encounter an ancient starglass mirror. Looking into it, you don't see your reflection, but a warm star-scape shifting with your calm breath. You realize you are in complete harmony.",
                choiceA = "", choiceB = ""
            ),
            OfflineStoryNode(
                id = 3, // From choice B
                text = "The suspension bridge sways gently under your feet, feeling like a cradle. In the deep woods, glowing lantern flowers light your path. The ancient trees hum a low, comforting song. You sit on a root, completely shielded from any earthly worry, finding inner peace.",
                choiceA = "", choiceB = ""
            )
        ),
        "Cyberpunk Lantern Garden" to listOf(
            OfflineStoryNode(
                id = 1,
                text = "Rain falls silently in a hyper-detailed alleyway, but every drop glows with a soft, warm amber violet light. You stand before a neon-glowing bio-garden where holographic cherry blossoms drift gently. The bustling city drone fades into absolute silence as you enter. A cyber-monk invites you to drink hot lavender tea.",
                choiceA = "Sit down immediately and close your eyes to listen to the glowing rain",
                choiceB = "Inquire about the digital lotus blooming inside an aqua-computer core"
            ),
            OfflineStoryNode(
                id = 2, // From A
                text = "You sit on a bamboo tatami. Close your eyes. The sound of rain on polymer rooftops aligns with your respiration. The lavender tea warms your hands and centers your mind. Every digital stress vanishes into beautiful, empty analog space.",
                choiceA = "", choiceB = ""
            ),
            OfflineStoryNode(
                id = 3, // From B
                text = "The cyber-monk smiles and points to the aqua core near the cherry tree. The digital lotus expands and contracts, simulating a breathing rhythm. As you synchronized your eyes and lungs with its cycles, a wave of digital calm washes over you.",
                choiceA = "", choiceB = ""
            )
        )
    )

    data class OfflineStoryNode(
        val id: Int,
        val text: String,
        val choiceA: String,
        val choiceB: String
    )

    fun startAIOfflineStory(theme: String) {
        val nodes = offlineStories[theme] ?: offlineStories.values.first()
        val startNode = nodes.first()
        _storyState.value = StoryState.Active(
            storyText = startNode.text,
            choices = listOf(startNode.choiceA, startNode.choiceB),
            history = theme
        )
    }

    fun makeStoryChoice(choiceIndex: Int, isOffline: Boolean) {
        val currentState = _storyState.value
        if (currentState !is StoryState.Active) return

        if (isOffline) {
            val theme = currentState.history
            val nodes = offlineStories[theme] ?: return
            val nextNode = if (choiceIndex == 0) nodes[1] else nodes[2]
            _storyState.value = StoryState.Conclusion(nextNode.text)
            viewModelScope.launch {
                repository.updateStreakAndPoints(15, isMindful = true)
            }
        } else {
            // Use Gemini API to continue the story
            _storyState.value = StoryState.Loading
            viewModelScope.launch {
                val chosenText = currentState.choices.getOrNull(choiceIndex) ?: "the next path"
                val prompt = """
                    You are continuing an interactive, highly calming and mindful short story.
                    The user previously read: "${currentState.storyText}"
                    And selected the choice: "$chosenText"
                    
                    Write the conclusion of this story in exactly one paragraph. Make it extremely relaxing, cozy, sensory, and beautiful. End with a warm reflection that guides them back to a calm state of presence. Do NOT offer any more selections. Keep it strictly underneath 130 words.
                """.trimIndent()
                
                val responseText = GeminiClient.generateStoryOrPrompt(prompt)
                if (responseText.isNotEmpty()) {
                    _storyState.value = StoryState.Conclusion(responseText)
                    repository.updateStreakAndPoints(20, isMindful = true)
                } else {
                    // Fallback to offline conclusion
                    val nodes = offlineStories[currentState.history] ?: offlineStories.values.first()
                    val nextNode = if (choiceIndex == 0) nodes[1] else nodes[2]
                    _storyState.value = StoryState.Conclusion(nextNode.text)
                    repository.updateStreakAndPoints(15, isMindful = true)
                }
            }
        }
    }

    fun startStoryChallenge(theme: String) {
        _storyState.value = StoryState.Loading
        viewModelScope.launch {
            val prompt = """
                You are a mindful story weaver. Write the introduction of an interactive short story based on the theme "$theme".
                The narrative must be exceptionally sensory, calming, visual, and grounded. 
                Keep it under 100 words.
                At the very end of your response, provide exactly two interactive, mindful routes for the player, formatted strictly on new lines at the bottom of the response like this:
                --- Choice A: [Your choice here]
                --- Choice B: [Your choice here]
                Ensure the flow is eye-safe and peaceful.
            """.trimIndent()

            val response = GeminiClient.generateStoryOrPrompt(prompt)
            if (response.isNotEmpty() && response.contains("--- Choice A:") && response.contains("--- Choice B:")) {
                // Parse Choices
                try {
                    val parts = response.split("--- Choice A:")
                    val storyText = parts[0].trim()
                    val secondPart = parts[1].split("--- Choice B:")
                    val choiceA = secondPart[0].trim()
                    val choiceB = secondPart[1].trim()

                    _storyState.value = StoryState.Active(
                        storyText = storyText,
                        choices = listOf(choiceA, choiceB),
                        history = theme
                    )
                } catch (e: Exception) {
                    // Parse failed, fallback
                    startAIOfflineStory(theme)
                }
            } else {
                // API keys empty or network failure, use beautiful offline stories
                startAIOfflineStory(theme)
            }
        }
    }

    fun resetStory() {
        _storyState.value = StoryState.Idle
    }

    // Dynamic Companion Reflection
    fun processReflection(userPrompt: String, userText: String, mood: String) {
        _aiReflectionFeedback.value = null
        _isAiLoading.value = true
        viewModelScope.launch {
            // Save the log immediately in Room
            val log = MindfulLog(prompt = userPrompt, userResponse = userText, mood = mood)
            repository.insertLog(log)
            repository.updateStreakAndPoints(15, isMindful = true)

            val prompt = """
                You are a gentle mindfulness companion. The user was prompted about: "$userPrompt"
                Their reflection was: "$userText"
                Their chosen current mood check-in is: "$mood"
                
                Write a soothing, 2-to-3 sentence companion response. Acknowledge their feelings with validation and serenity. Provide a single, quick physical anchor step (like releasing shoulders or feeling back posture) they can do right now. Keep your tone quiet, cozy, and validating.
            """.trimIndent()

            val response = GeminiClient.generateStoryOrPrompt(prompt)
            _isAiLoading.value = false
            if (response.isNotEmpty()) {
                _aiReflectionFeedback.value = response
            } else {
                // Elegant local offline comforting wisdom
                val offlineResp = when (mood) {
                    "Calm" -> "It is wonderful to feel anchored and still. Allow this soothing energy to settle warm and deep within your chest."
                    "Anxious" -> "Your breath is an open shore. Let the tension cascade off your shoulders with your next deep exhale. You are safe here."
                    "Excited" -> "It is beautiful to celebrate this vibrant energy. Enjoy the bright shimmer of life while keeping your breath slow and paced."
                    "Tired" -> "Give your heavy eyes permission to soften. Your body has worked so hard; allow this moment to be a quiet cushion."
                    else -> "No matter what ripples pass through your mind, you are sitting quietly with yourself, which is a rare and beautiful gift."
                } + " Take a brief moment to draw a deep, slow breath, raising your collarbones, and release it completely."
                _aiReflectionFeedback.value = offlineResp
            }
        }
    }

    fun clearReflectionState() {
        _aiReflectionFeedback.value = null
    }

    // Scoring & Streaks
    fun addPoints(points: Int, isMindful: Boolean) {
        viewModelScope.launch {
            repository.updateStreakAndPoints(points, isMindful)
        }
    }

    fun addLeaderboardEntry(name: String, score: Int) {
        viewModelScope.launch {
            repository.addLeaderboardEntry(name, score, isUser = true)
        }
    }
}

class MindPlayViewModelFactory(private val repository: MindPlayRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MindPlayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MindPlayViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
