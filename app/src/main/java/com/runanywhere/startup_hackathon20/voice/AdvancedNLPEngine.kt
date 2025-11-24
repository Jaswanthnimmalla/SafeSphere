package com.runanywhere.startup_hackathon20.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * Advanced NLP Engine for Voice Control
 *
 * Features:
 * - Intent Classification with confidence scoring
 * - Named Entity Recognition (NER)
 * - Context-aware interpretation
 * - Multi-turn dialogue management
 * - Sentiment analysis for security
 * - Learning from corrections
 */
class AdvancedNLPEngine(private val context: Context) {

    private val _conversationContext = MutableStateFlow<ConversationContext>(ConversationContext())
    val conversationContext: StateFlow<ConversationContext> = _conversationContext

    private val _learningData = MutableStateFlow<List<LearningEntry>>(emptyList())
    val learningData: StateFlow<List<LearningEntry>> = _learningData

    // Intent patterns with multiple variations and context
    private val intentPatterns = mapOf(
        VoiceIntent.NAVIGATE_DASHBOARD to listOf(
            // Standard commands
            Regex("(?i).*(go|open|show|navigate|take me|bring me|move).*?(home|dashboard|main|start).*"),
            Regex("(?i).*(back to|return to|head to).*?(home|dashboard|main).*"),
            Regex("(?i)^(home|dashboard|main screen)$"),
            Regex("(?i).*(what's|show me).*?(my|the)? ?(overview|summary).*"),
            // Casual/Random variations
            Regex("(?i).*(go|move|take me|bring me).*(back|to)? ?(home page|homepage|main page).*"),
            Regex("(?i).*(show|display|let me see).*(main|home|start) ?(screen|page)?.*"),
            Regex("(?i).*(i want|i wanna|i need).*(go|see).*(home|dashboard|main).*"),
            Regex("(?i).*(open up|pull up).*(home|dashboard|main) ?(screen|page)?.*"),
            Regex("(?i).*(take me|bring me|get me).*(to|back to)? ?(home|main|dashboard).*"),
            Regex("(?i).*home ?(page|screen)?$"),
            Regex("(?i).*dashboard$"),
            Regex("(?i).*(go|back).*(to)? ?(the)? ?(main|home|start) ?(screen|page)?.*")
        ),
        VoiceIntent.NAVIGATE_PASSWORDS to listOf(
            // Standard commands
            Regex("(?i).*(show|open|view|display|list).*?(my )?passwords.*"),
            Regex("(?i).*(password|credentials).*?(manager|vault|list).*"),
            Regex("(?i).*(go to|navigate to).*?password.*"),
            Regex("(?i).*(find|search).*?password.*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(see|view|check).*(my )?passwords.*"),
            Regex("(?i).*(open up|pull up|bring up).*(password|passwords) ?(manager|screen|page)?.*"),
            Regex("(?i).*(where|show me).*(my )?passwords.*"),
            Regex("(?i).*(go to|move to|take me to).*(password|passwords) ?(section|area|page|screen)?.*"),
            Regex("(?i).*(let me see|show me).*(my )?password ?(list|manager)?.*"),
            Regex("(?i).*(check|look at).*(my )?passwords.*"),
            Regex("(?i).*(password|passwords) ?(page|screen|$)"),
            Regex("(?i).*(my|the) ?(password|passwords)$"),
            Regex("(?i).*(open|show).*(password area|password section).*"),
            Regex("(?i).*(i want to|let me).*(manage|see|view).*(my )?passwords.*")
        ),
        VoiceIntent.NAVIGATE_VAULT to listOf(
            // Standard commands
            Regex("(?i).*(open|show|access).*?(privacy )?vault.*"),
            Regex("(?i).*(my )?vault.*"),
            Regex("(?i).*(secure|encrypted).*?(storage|files|documents).*"),
            Regex("(?i).*(go to|show me).*?vault.*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(see|open|check).*(my )?vault.*"),
            Regex("(?i).*(open up|pull up|bring up).*(vault|privacy vault|secure storage).*"),
            Regex("(?i).*(where|show me).*(my )?vault.*"),
            Regex("(?i).*(go to|move to|take me to).*(vault|privacy vault) ?(section|area|page|screen)?.*"),
            Regex("(?i).*(let me see|show me).*(my )?vault.*"),
            Regex("(?i).*(check|look at).*(my )?vault.*"),
            Regex("(?i).*(vault|secure storage) ?(page|screen|$)"),
            Regex("(?i).*(my|the) ?(vault|secure storage|encrypted storage)$"),
            Regex("(?i).*(open|show).*(vault area|vault section|secure area).*"),
            Regex("(?i).*(private|encrypted|secure) ?(files|storage|vault).*")
        ),
        VoiceIntent.NAVIGATE_SETTINGS to listOf(
            // Standard commands
            Regex("(?i).*(open|show|go to).*?settings.*"),
            Regex("(?i).*(change|modify|update).*?(settings|preferences|options).*"),
            Regex("(?i).*(configure|setup).*?(app|application).*"),
            Regex("(?i)^settings$"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(change|modify|update|see).*(settings|preferences|options).*"),
            Regex("(?i).*(open up|pull up|bring up).*(settings|preferences|options) ?(screen|page)?.*"),
            Regex("(?i).*(where|show me).*(settings|preferences|options).*"),
            Regex("(?i).*(go to|move to|take me to).*(settings|preferences|options) ?(section|area|page|screen)?.*"),
            Regex("(?i).*(let me see|show me).*(settings|preferences|configuration).*"),
            Regex("(?i).*(app|application) ?(settings|preferences|options|configuration).*"),
            Regex("(?i).*(settings|preferences|options) ?(page|screen|$)"),
            Regex("(?i).*(my|the) ?(settings|preferences)$"),
            Regex("(?i).*(adjust|modify|change).*(app|application) ?(settings)?.*")
        ),
        VoiceIntent.NAVIGATE_AI_CHAT to listOf(
            // Standard commands
            Regex("(?i).*(talk to|chat with|open).*?(ai|assistant|bot).*"),
            Regex("(?i).*(ai|assistant).*?(chat|conversation).*"),
            Regex("(?i).*(ask|speak to).*?(ai|assistant).*"),
            Regex("(?i).*(privacy|security).*?(advisor|assistant).*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(talk to|chat with|speak to).*(ai|assistant|bot).*"),
            Regex("(?i).*(open up|pull up|bring up).*(ai|assistant|chat) ?(screen|page)?.*"),
            Regex("(?i).*(where|show me).*(ai|assistant|chat).*"),
            Regex("(?i).*(go to|move to|take me to).*(ai|assistant|chat) ?(section|area|page|screen)?.*"),
            Regex("(?i).*(let me).*(talk to|chat with|speak to).*(ai|assistant).*"),
            Regex("(?i).*(ai|assistant|bot) ?(chat|conversation)$"),
            Regex("(?i).*(chat|talk|speak).*(with)? ?(ai|assistant|bot).*"),
            Regex("(?i).*(message|ask).*(ai|assistant|bot).*"),
            Regex("(?i).*(open|show).*(chat|conversation|ai).*")
        ),
        VoiceIntent.CHECK_SECURITY to listOf(
            // Standard commands
            Regex("(?i).*(check|analyze|scan|review).*?(security|safety).*"),
            Regex("(?i).*(how|what).*(secure|safe|protected).*"),
            Regex("(?i).*(security|safety).*?(check|status|score|report).*"),
            Regex("(?i).*(am i|is my).*?(safe|secure|protected).*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(check|see|know).*(security|safety|how secure|how safe).*"),
            Regex("(?i).*(tell me|show me).*(security|safety) ?(status|score)?.*"),
            Regex("(?i).*(how|what).*(my)? ?(security|safety) ?(status|score|level)?.*"),
            Regex("(?i).*(am i|is it).*(safe|secure|protected|ok|okay).*"),
            Regex("(?i).*(security|safety) ?(check|scan|status|score|analysis).*"),
            Regex("(?i).*(check|verify|test).*(my)? ?(security|safety|protection).*"),
            Regex("(?i).*(run|do).*(security|safety) ?(check|scan|test|analysis).*"),
            Regex("(?i).*(how secure|how safe).*(am i|is my|is it).*"),
            Regex("(?i).*(protected|secure|safe).*(am i|is my data|is it)?.*"),
            Regex("(?i).*(security|safety).*(report|summary|overview).*")
        ),
        VoiceIntent.AI_PREDICTOR to listOf(
            // Standard commands
            Regex("(?i).*(predict|forecast|anticipate).*?(threat|risk|breach).*"),
            Regex("(?i).*(ai|ml).*?(predictor|prediction|forecast).*"),
            Regex("(?i).*(future|upcoming).*?(risk|threat|vulnerability).*"),
            Regex("(?i).*(show|tell me).*?prediction.*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(see|check|know).*(prediction|forecast|future).*"),
            Regex("(?i).*(open up|pull up|bring up).*(predictor|prediction|forecast).*"),
            Regex("(?i).*(what|show me).*(future|upcoming) ?(risk|threat|vulnerability|prediction).*"),
            Regex("(?i).*(go to|move to|take me to).*(predictor|prediction|forecast) ?(screen|page)?.*"),
            Regex("(?i).*(let me see|show me).*(ai|ml) ?(predictor|prediction|forecast).*"),
            Regex("(?i).*(predictor|prediction|forecast) ?(page|screen|$)"),
            Regex("(?i).*(predict|forecast).*(future|upcoming) ?(risk|threat).*"),
            Regex("(?i).*(what|show).*(will|might|could) ?(happen|come|occur).*"),
            Regex("(?i).*(future|upcoming) ?(security|safety) ?(risk|threat|prediction).*")
        ),
        VoiceIntent.ADD_PASSWORD to listOf(
            // Standard commands
            Regex("(?i).*(add|create|save|store).*?(new )?password.*"),
            Regex("(?i).*(new|add).*?(login|credential|account).*"),
            Regex("(?i).*(save|store).*?(login|credential).*?(for|to).*"),
            Regex("(?i).*(remember|keep).*?password.*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(add|create|save|store).*(new |another )?password.*"),
            Regex("(?i).*(let me).*(add|create|save|store).*(password|login|credential).*"),
            Regex("(?i).*(add|create|save|store).*(new |another )?(password|login|credential|account).*"),
            Regex("(?i).*(i have|i got).*(new |another )?(password|login|account).*(save|store|add).*"),
            Regex("(?i).*(save|store|add|remember).*(this|my|a new) ?(password|login|credential).*"),
            Regex("(?i).*(new|another) ?(password|login|account).*"),
            Regex("(?i).*(make|create).*(new |another )?(password|login) ?(entry)?.*")
        ),
        VoiceIntent.SEARCH_PASSWORD to listOf(
            // Standard commands
            Regex("(?i).*(find|search|look for|get).*?password.*?(for|of)?.*"),
            Regex("(?i).*(what's|show me|tell me).*?password.*?(for|of)?.*"),
            Regex("(?i).*(password|login).*?(for|of).*"),
            Regex("(?i).*(retrieve|get).*?(my )?password.*"),
            // Casual/Random variations - GMAIL, FACEBOOK, TWITTER specific
            Regex("(?i).*(open|show|get|find).*(my |the )?(gmail|google|email) ?(password|login|account).*"),
            Regex("(?i).*(what's|whats|show me|tell me).*(my |the )?(gmail|google|email) ?(password|login).*"),
            Regex("(?i).*(i need|i want).*(my |the )?(gmail|google|email) ?(password|login|account).*"),
            Regex("(?i).*(open|show|get|find).*(my |the )?(facebook|fb) ?(password|login|account).*"),
            Regex("(?i).*(what's|whats|show me|tell me).*(my |the )?(facebook|fb) ?(password|login).*"),
            Regex("(?i).*(i need|i want).*(my |the )?(facebook|fb) ?(password|login|account).*"),
            Regex("(?i).*(open|show|get|find).*(my |the )?(twitter|tweet) ?(password|login|account).*"),
            Regex("(?i).*(what's|whats|show me|tell me).*(my |the )?(twitter|tweet) ?(password|login).*"),
            Regex("(?i).*(i need|i want).*(my |the )?(twitter|tweet) ?(password|login|account).*"),
            // General search variations
            Regex("(?i).*(i want|i wanna|i need).*(find|see|get|check).*(password|login|credential).*"),
            Regex("(?i).*(where|show me).*(password|login).*?(for|of)?.*"),
            Regex("(?i).*(look up|find out|get).*(password|login).*?(for|of)?.*"),
            Regex("(?i).*(search|find|look for).*(password|login|credential).*"),
            Regex("(?i).*(password|login).*?(for|of|to).*(what|which|where).*"),
            Regex("(?i).*(give me|show me|tell me).*(password|login).*")
        ),
        VoiceIntent.GENERATE_PASSWORD to listOf(
            // Standard commands
            Regex("(?i).*(generate|create|make).*?(strong|secure|random|new).*?password.*"),
            Regex("(?i).*(password|pass).*?(generator|generation).*"),
            Regex("(?i).*(new|random|secure).*?password.*"),
            Regex("(?i).*(suggest|recommend).*?password.*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(generate|create|make).*(strong|secure|random|good) ?(password)?.*"),
            Regex("(?i).*(generate|create|make|give me).*(strong|secure|random|new|good) ?(password)?.*"),
            Regex("(?i).*(i need|i want).*(strong|secure|random|good|new) ?(password)?.*"),
            Regex("(?i).*(make|create|generate).*(password|pass).*?(for me)?.*"),
            Regex("(?i).*(random|strong|secure|good) ?(password|pass) ?(for me|please)?.*"),
            Regex("(?i).*(suggest|recommend|give).*(password|pass).*"),
            Regex("(?i).*(password|pass) ?(generator|maker|creator).*"),
            Regex("(?i).*(help me).*(make|create|generate).*(password)?.*")
        ),
        VoiceIntent.LOCK_APP to listOf(
            // Standard commands
            Regex("(?i).*(lock|secure|close).*?(app|application|safesphere).*"),
            Regex("(?i).*(log ?out|sign ?out).*"),
            Regex("(?i).*(activate|enable|turn on).*?lock.*"),
            Regex("(?i)^(lock|secure)( it| app)?$"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(lock|secure|close).*(app|application|this)?.*"),
            Regex("(?i).*(lock|secure|close).*(up|down|this|it|the app|safesphere)?.*"),
            Regex("(?i).*(log|sign) ?(me )?(out|off).*"),
            Regex("(?i).*(exit|leave|close).*(app|application|safesphere)?.*"),
            Regex("(?i).*(turn on|enable|activate).*(lock|security).*"),
            Regex("(?i).*(lock|secure).*(it|this|the app|safesphere|everything)?$"),
            Regex("(?i).*(make it|keep it) ?(secure|locked|safe).*")
        ),
        VoiceIntent.BACKUP_DATA to listOf(
            // Standard commands
            Regex("(?i).*(backup|save|export).*?(my )?data.*"),
            Regex("(?i).*(create|make).*?backup.*"),
            Regex("(?i).*(export|save).*?(vault|passwords|data).*"),
            Regex("(?i).*(backup|save).*?(everything|all).*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(backup|save|export).*(my )?data.*"),
            Regex("(?i).*(backup|save|export).*(everything|all|my stuff|my data).*"),
            Regex("(?i).*(make|create|do).*(backup|copy).*"),
            Regex("(?i).*(save|export).*(vault|passwords|data|everything).*"),
            Regex("(?i).*(i need|i want).*(copy|backup|save).*(data|passwords|vault).*")
        ),
        VoiceIntent.CHECK_BREACHES to listOf(
            // Standard commands
            Regex("(?i).*(check|scan|search).*?(for )?breach(es|ed)?.*"),
            Regex("(?i).*(any|am i|have i).*?breach(es|ed)?.*"),
            Regex("(?i).*(compromised|leaked|exposed).*?(password|account|data).*"),
            Regex("(?i).*(breach|leak).*?(check|scan|detection).*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i wanna|i need).*(check|see|know).*(breach|leak|compromised).*"),
            Regex("(?i).*(am i|is my|have i been).*?(breached|leaked|compromised|hacked|exposed).*"),
            Regex("(?i).*(check|scan|search).*(for)? ?(breach|leak|hack|compromise).*"),
            Regex("(?i).*(any|were there|are there).*?(breach|leak|hack|compromise).*"),
            Regex("(?i).*(data|password|account).*?(breach|leak|hack|compromised|exposed).*"),
            Regex("(?i).*(tell me|show me).*(breach|leak|hack|compromise).*")
        ),
        VoiceIntent.VOICE_HELP to listOf(
            // Standard commands
            Regex("(?i).*(help|assist|what can).*?(me|you do|commands).*"),
            Regex("(?i).*(commands|voice control|what).*(available|can i say).*"),
            Regex("(?i).*(how|what).*?(use|control).*?voice.*"),
            Regex("(?i)^(help|commands|what can you do)$"),
            // Casual/Random variations
            Regex("(?i).*(i need|i want).*(help|assistance|support).*"),
            Regex("(?i).*(what|which).*(commands|things).*(can i|do you|you have).*"),
            Regex("(?i).*(tell me|show me).*(commands|what you can do|options).*"),
            Regex("(?i).*(how|what).*(do i|can i).*(use|say|speak|command).*"),
            Regex("(?i).*(help|assist|support) ?(me|please)?$"),
            Regex("(?i).*(what|how).*(works|do|say).*"),
            Regex("(?i).*(available|possible) ?(commands|options|things).*")
        ),
        VoiceIntent.READ_ALOUD to listOf(
            // Standard commands
            Regex("(?i).*(read|speak|say).*?(aloud|out loud).*"),
            Regex("(?i).*(tell me|read).*?(what|content).*"),
            Regex("(?i).*(read|say).*?(this|that|screen).*"),
            Regex("(?i).*(voice|speak).*?(output|response).*"),
            // Casual/Random variations
            Regex("(?i).*(read|speak|say).*(to me|for me|this|that).*"),
            Regex("(?i).*(i want|i need).*(hear|listen).*(this|that|it).*"),
            Regex("(?i).*(what|tell me).*(says|is on|is written).*"),
            Regex("(?i).*(speak|read|say).*(screen|content|text).*")
        ),
        VoiceIntent.ENABLE_VOICE_CONTROL to listOf(
            // Standard commands
            Regex("(?i).*(enable|activate|turn on|start).*?voice.*?(control|command).*"),
            Regex("(?i).*(voice|speech).*?(on|enable|activate).*"),
            Regex("(?i).*(start|begin).*?(listening|voice mode).*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i need).*(enable|activate|turn on|start).*(voice)?.*"),
            Regex("(?i).*(turn on|enable|activate|start).*(voice|speech|listening).*"),
            Regex("(?i).*(voice|speech).*(on|enabled|activated).*"),
            Regex("(?i).*(start|begin).*(voice|listening|speech).*")
        ),
        VoiceIntent.DISABLE_VOICE_CONTROL to listOf(
            // Standard commands
            Regex("(?i).*(disable|deactivate|turn off|stop).*?voice.*?(control|command).*"),
            Regex("(?i).*(voice|speech).*?(off|disable|deactivate).*"),
            Regex("(?i).*(stop|end).*?(listening|voice mode).*"),
            // Casual/Random variations
            Regex("(?i).*(i want|i need).*(disable|deactivate|turn off|stop).*(voice)?.*"),
            Regex("(?i).*(turn off|disable|deactivate|stop).*(voice|speech|listening).*"),
            Regex("(?i).*(voice|speech).*(off|disabled|deactivated).*"),
            Regex("(?i).*(stop|end|quit).*(voice|listening|speech).*"),
            Regex("(?i).*(shut up|be quiet|stop talking).*")
        )
    )

    // Entity extraction patterns - EXPANDED for more services
    private val entityPatterns = mapOf(
        EntityType.APP_NAME to Regex("(?i)(facebook|google|twitter|amazon|netflix|github|gmail|instagram|linkedin|microsoft|apple|spotify|youtube|reddit|pinterest|snapchat|tiktok|whatsapp|telegram|discord|slack|zoom|dropbox|paypal|ebay|airbnb|uber|lyft|booking|expedia|steam|epic|origin|battlenet)"),
        EntityType.WEBSITE to Regex("(?i)([a-zA-Z0-9-]+\\.(com|net|org|io|co|app|dev|uk|ca|au|in))"),
        EntityType.EMAIL to Regex("(?i)([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"),
        EntityType.USERNAME to Regex("(?i)(username|user|login|account)\\s+(?:is\\s+)?([a-zA-Z0-9._-]+)"),
        EntityType.NUMBER to Regex("\\b(\\d+)\\b"),
        EntityType.DATE to Regex("(?i)(today|yesterday|tomorrow|last\\s+week|next\\s+week|\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})"),
        EntityType.TIME to Regex("(?i)(\\d{1,2}:\\d{2}\\s*(?:am|pm)?|morning|afternoon|evening|night)"),
        EntityType.DURATION to Regex("(?i)(\\d+)\\s*(second|minute|hour|day|week|month|year)s?")
    )

    // Sentiment keywords for security context
    private val positiveKeywords = setOf(
        "good", "great", "excellent", "safe", "secure", "protected", "strong",
        "perfect", "best", "wonderful", "amazing", "fantastic"
    )

    private val negativeKeywords = setOf(
        "bad", "weak", "poor", "unsafe", "insecure", "vulnerable", "compromised",
        "breached", "leaked", "exposed", "danger", "threat", "risk", "attack"
    )

    private val urgentKeywords = setOf(
        "urgent", "emergency", "critical", "immediately", "now", "asap",
        "quick", "fast", "hurry", "important", "priority"
    )

    /**
     * Parse voice input with advanced NLP
     */
    fun parseCommand(input: String): NLPResult {
        val normalizedInput = normalizeInput(input)

        // Extract entities first
        val entities = extractEntities(normalizedInput)

        // Classify intent with confidence scoring
        val intentMatch = classifyIntent(normalizedInput, entities)

        // Analyze sentiment
        val sentiment = analyzeSentiment(normalizedInput)

        // Apply context from previous turns
        val contextualIntent = applyContext(intentMatch, entities)

        // Update conversation context
        updateContext(contextualIntent, entities, normalizedInput)

        return NLPResult(
            intent = contextualIntent.intent,
            confidence = contextualIntent.confidence,
            entities = entities,
            sentiment = sentiment,
            originalInput = input,
            normalizedInput = normalizedInput,
            contextUsed = _conversationContext.value.lastIntent != null
        )
    }

    /**
     * Normalize input text
     */
    private fun normalizeInput(input: String): String {
        return input.trim()
            .replace(Regex("\\s+"), " ") // Multiple spaces to single
            .replace(Regex("[?!.]+$"), "") // Remove trailing punctuation
    }

    /**
     * Classify intent with confidence scoring
     */
    private fun classifyIntent(
        input: String,
        entities: Map<EntityType, List<String>>
    ): IntentMatch {
        val matches = mutableListOf<Pair<VoiceIntent, Double>>()

        for ((intent, patterns) in intentPatterns) {
            for (pattern in patterns) {
                if (pattern.containsMatchIn(input)) {
                    // Base confidence from pattern match
                    var confidence = 0.7

                    // Boost confidence based on entity presence
                    confidence += when (intent) {
                        VoiceIntent.SEARCH_PASSWORD, VoiceIntent.ADD_PASSWORD -> {
                            if (entities.containsKey(EntityType.APP_NAME) ||
                                entities.containsKey(EntityType.WEBSITE) ||
                                entities.containsKey(EntityType.EMAIL)
                            ) 0.2 else 0.0
                        }

                        VoiceIntent.GENERATE_PASSWORD -> {
                            if (entities.containsKey(EntityType.NUMBER)) 0.15 else 0.0
                        }

                        else -> 0.0
                    }

                    // Boost for exact keyword matches
                    val exactKeywords = getExactKeywordsForIntent(intent)
                    val keywordMatches =
                        exactKeywords.count { input.contains(it, ignoreCase = true) }
                    confidence += keywordMatches * 0.05

                    // Cap at 1.0
                    confidence = confidence.coerceAtMost(1.0)

                    matches.add(intent to confidence)
                    break // Only first pattern match per intent
                }
            }
        }

        // Return highest confidence match, or UNKNOWN
        return if (matches.isNotEmpty()) {
            val best = matches.maxByOrNull { it.second }!!
            IntentMatch(best.first, best.second)
        } else {
            IntentMatch(VoiceIntent.UNKNOWN, 0.0)
        }
    }

    /**
     * Get exact keywords that should boost confidence for an intent
     */
    private fun getExactKeywordsForIntent(intent: VoiceIntent): List<String> {
        return when (intent) {
            VoiceIntent.NAVIGATE_PASSWORDS -> listOf(
                "password",
                "passwords",
                "credentials",
                "logins"
            )

            VoiceIntent.NAVIGATE_VAULT -> listOf("vault", "secure", "encrypted", "privacy")
            VoiceIntent.CHECK_SECURITY -> listOf("security", "safety", "check", "scan", "analyze")
            VoiceIntent.AI_PREDICTOR -> listOf("predict", "forecast", "future", "ai")
            VoiceIntent.GENERATE_PASSWORD -> listOf("generate", "create", "random", "strong")
            VoiceIntent.CHECK_BREACHES -> listOf("breach", "leaked", "compromised", "exposed")
            else -> emptyList()
        }
    }

    /**
     * Extract named entities from input
     */
    private fun extractEntities(input: String): Map<EntityType, List<String>> {
        val entities = mutableMapOf<EntityType, MutableList<String>>()

        for ((type, pattern) in entityPatterns) {
            val matches = pattern.findAll(input)
            for (match in matches) {
                val value = when (type) {
                    EntityType.USERNAME -> match.groupValues.getOrNull(2) ?: match.value
                    else -> match.groupValues.getOrNull(1) ?: match.value
                }

                entities.getOrPut(type) { mutableListOf() }.add(value.trim())
            }
        }

        return entities
    }

    /**
     * Analyze sentiment for security context
     */
    private fun analyzeSentiment(input: String): SentimentAnalysis {
        val words = input.lowercase().split(Regex("\\s+"))

        val positiveCount = words.count { it in positiveKeywords }
        val negativeCount = words.count { it in negativeKeywords }
        val urgentCount = words.count { it in urgentKeywords }

        val sentiment = when {
            negativeCount > positiveCount -> Sentiment.NEGATIVE
            positiveCount > negativeCount -> Sentiment.POSITIVE
            else -> Sentiment.NEUTRAL
        }

        val urgency = when {
            urgentCount > 0 -> Urgency.HIGH
            negativeCount > 1 -> Urgency.MEDIUM
            else -> Urgency.LOW
        }

        val confidence = when {
            positiveCount + negativeCount > 2 -> 0.9
            positiveCount + negativeCount > 0 -> 0.7
            else -> 0.5
        }

        return SentimentAnalysis(
            sentiment = sentiment,
            urgency = urgency,
            confidence = confidence,
            keywords = (words.filter { it in positiveKeywords || it in negativeKeywords || it in urgentKeywords })
        )
    }

    /**
     * Apply conversation context to improve intent classification
     */
    private fun applyContext(
        intentMatch: IntentMatch,
        entities: Map<EntityType, List<String>>
    ): IntentMatch {
        val context = _conversationContext.value

        // If low confidence and have context, try to infer from context
        if (intentMatch.confidence < 0.6 && context.lastIntent != null) {
            // Context-based inference patterns
            val contextualIntent = when (context.lastIntent) {
                VoiceIntent.NAVIGATE_PASSWORDS -> {
                    // If we just navigated to passwords, interpret ambiguous commands as password actions
                    when {
                        entities.containsKey(EntityType.APP_NAME) -> VoiceIntent.SEARCH_PASSWORD
                        entities.containsKey(EntityType.WEBSITE) -> VoiceIntent.SEARCH_PASSWORD
                        else -> intentMatch.intent
                    }
                }

                VoiceIntent.SEARCH_PASSWORD -> {
                    // After searching, user might want to add or generate
                    if (intentMatch.intent == VoiceIntent.UNKNOWN) {
                        VoiceIntent.ADD_PASSWORD
                    } else intentMatch.intent
                }

                VoiceIntent.CHECK_SECURITY -> {
                    // After security check, might want to see details
                    if (intentMatch.intent == VoiceIntent.UNKNOWN) {
                        VoiceIntent.NAVIGATE_PASSWORDS // Show passwords to fix issues
                    } else intentMatch.intent
                }

                else -> intentMatch.intent
            }

            // Boost confidence if we used context
            val boostedConfidence = if (contextualIntent != intentMatch.intent) {
                (intentMatch.confidence + 0.3).coerceAtMost(0.85)
            } else {
                intentMatch.confidence
            }

            return IntentMatch(contextualIntent, boostedConfidence)
        }

        return intentMatch
    }

    /**
     * Update conversation context
     */
    private fun updateContext(
        intent: IntentMatch,
        entities: Map<EntityType, List<String>>,
        input: String
    ) {
        val currentContext = _conversationContext.value

        _conversationContext.value = ConversationContext(
            lastIntent = intent.intent,
            lastEntities = entities,
            lastInput = input,
            turnCount = currentContext.turnCount + 1,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Learn from user corrections
     */
    fun learnFromCorrection(
        originalInput: String,
        interpretedIntent: VoiceIntent,
        correctIntent: VoiceIntent,
        feedback: String? = null
    ) {
        val entry = LearningEntry(
            input = originalInput,
            interpretedIntent = interpretedIntent,
            correctIntent = correctIntent,
            feedback = feedback,
            timestamp = System.currentTimeMillis()
        )

        _learningData.value = _learningData.value + entry

        // TODO: Use learning data to improve patterns (could use on-device ML model)
        android.util.Log.d("NLPEngine", "Learning entry added: $entry")
    }

    /**
     * Get suggestions based on partial input
     */
    fun getSuggestions(partialInput: String): List<VoiceSuggestion> {
        if (partialInput.length < 2) return emptyList()

        val suggestions = mutableListOf<VoiceSuggestion>()
        val normalized = partialInput.lowercase()

        // Common command suggestions
        val commonCommands = mapOf(
            "show" to listOf("show passwords", "show vault", "show security score"),
            "open" to listOf("open passwords", "open vault", "open settings"),
            "check" to listOf("check security", "check breaches", "check password health"),
            "generate" to listOf("generate password", "generate strong password"),
            "find" to listOf("find password for", "find my passwords"),
            "add" to listOf("add password", "add new password")
        )

        for ((prefix, commands) in commonCommands) {
            if (prefix.startsWith(normalized)) {
                commands.forEach { command ->
                    suggestions.add(
                        VoiceSuggestion(
                            text = command,
                            confidence = 0.9,
                            category = "Common Commands"
                        )
                    )
                }
            }
        }

        // Context-based suggestions
        val context = _conversationContext.value
        if (context.lastIntent != null) {
            val contextualSuggestions = getContextualSuggestions(context.lastIntent)
            suggestions.addAll(contextualSuggestions)
        }

        return suggestions.sortedByDescending { it.confidence }.take(5)
    }

    /**
     * Get suggestions based on conversation context
     */
    private fun getContextualSuggestions(lastIntent: VoiceIntent): List<VoiceSuggestion> {
        return when (lastIntent) {
            VoiceIntent.NAVIGATE_PASSWORDS -> listOf(
                VoiceSuggestion("add new password", 0.85, "Next Steps"),
                VoiceSuggestion("search password for", 0.80, "Next Steps"),
                VoiceSuggestion("generate strong password", 0.75, "Next Steps")
            )

            VoiceIntent.CHECK_SECURITY -> listOf(
                VoiceSuggestion("show password health", 0.85, "Next Steps"),
                VoiceSuggestion("check for breaches", 0.80, "Next Steps"),
                VoiceSuggestion("view weak passwords", 0.75, "Next Steps")
            )

            VoiceIntent.SEARCH_PASSWORD -> listOf(
                VoiceSuggestion("copy password", 0.85, "Next Steps"),
                VoiceSuggestion("edit password", 0.80, "Next Steps"),
                VoiceSuggestion("delete password", 0.70, "Next Steps")
            )

            else -> emptyList()
        }
    }

    /**
     * Clear conversation context (e.g., after timeout or screen change)
     */
    fun clearContext() {
        _conversationContext.value = ConversationContext()
    }
}

// Data Models

data class NLPResult(
    val intent: VoiceIntent,
    val confidence: Double,
    val entities: Map<EntityType, List<String>>,
    val sentiment: SentimentAnalysis,
    val originalInput: String,
    val normalizedInput: String,
    val contextUsed: Boolean
)

data class IntentMatch(
    val intent: VoiceIntent,
    val confidence: Double
)

data class ConversationContext(
    val lastIntent: VoiceIntent? = null,
    val lastEntities: Map<EntityType, List<String>> = emptyMap(),
    val lastInput: String = "",
    val turnCount: Int = 0,
    val timestamp: Long = 0L
)

data class SentimentAnalysis(
    val sentiment: Sentiment,
    val urgency: Urgency,
    val confidence: Double,
    val keywords: List<String>
)

enum class Sentiment {
    POSITIVE, NEGATIVE, NEUTRAL
}

enum class Urgency {
    LOW, MEDIUM, HIGH
}

enum class EntityType {
    APP_NAME,
    WEBSITE,
    EMAIL,
    USERNAME,
    NUMBER,
    DATE,
    TIME,
    DURATION
}

data class LearningEntry(
    val input: String,
    val interpretedIntent: VoiceIntent,
    val correctIntent: VoiceIntent,
    val feedback: String?,
    val timestamp: Long
)

data class VoiceSuggestion(
    val text: String,
    val confidence: Double,
    val category: String
)

// Voice Intents
enum class VoiceIntent {
    NAVIGATE_DASHBOARD,
    NAVIGATE_PASSWORDS,
    NAVIGATE_VAULT,
    NAVIGATE_SETTINGS,
    NAVIGATE_AI_CHAT,
    CHECK_SECURITY,
    AI_PREDICTOR,
    ADD_PASSWORD,
    SEARCH_PASSWORD,
    GENERATE_PASSWORD,
    LOCK_APP,
    BACKUP_DATA,
    CHECK_BREACHES,
    VOICE_HELP,
    READ_ALOUD,
    ENABLE_VOICE_CONTROL,
    DISABLE_VOICE_CONTROL,
    UNKNOWN
}
