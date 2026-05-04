package com.example.contentanalyzer.domain.utils

import android.util.Log

/**
 * Text Input Validator for AI Content Analysis
 * Ensures text meets minimum requirements for reliable detection
 */
class TextInputValidator {

    companion object {
        private const val TAG = "TextInputValidator"

        // Minimum word counts for different confidence levels
        private const val MIN_WORDS_ABSOLUTE = 10          // Absolute minimum
        private const val MIN_WORDS_LOW_CONFIDENCE = 20     // Very low confidence
        private const val MIN_WORDS_ACCEPTABLE = 50         // Acceptable minimum
        private const val MIN_WORDS_RECOMMENDED = 100       // Recommended for accuracy
        private const val MIN_WORDS_OPTIMAL = 200           // Optimal for best results

        // Character count equivalents (approx)
        private const val MIN_CHARS_ABSOLUTE = 50
        private const val MIN_CHARS_RECOMMENDED = 300
    }

    /**
     * Main validation function
     * Returns comprehensive validation result with recommendations
     */
    fun validateText(text: String): ValidationResult {
        val trimmedText = text.trim()

        if (trimmedText.isEmpty()) {
            return ValidationResult(
                isValid = false,
                status = ValidationStatus.EMPTY,
                message = "Please enter text to analyze",
                recommendation = "Enter at least $MIN_WORDS_RECOMMENDED words for accurate results",
                wordCount = 0,
                charCount = 0,
                confidenceMultiplier = 0f,
                needsMoreText = true
            )
        }

        val wordCount = countWords(trimmedText)
        val charCount = trimmedText.length
        val sentenceCount = countSentences(trimmedText)

        // Determine validation status based on word count
        val (status, isValid, confidenceMultiplier) = when {
            wordCount < MIN_WORDS_ABSOLUTE -> {
                Triple(ValidationStatus.TOO_SHORT_ABSOLUTE, false, 0f)
            }
            wordCount < MIN_WORDS_LOW_CONFIDENCE -> {
                Triple(ValidationStatus.TOO_SHORT_LOW_CONFIDENCE, false, 0.2f)
            }
            wordCount < MIN_WORDS_ACCEPTABLE -> {
                Triple(ValidationStatus.TOO_SHORT_ACCEPTABLE, false, 0.4f)
            }
            wordCount < MIN_WORDS_RECOMMENDED -> {
                Triple(ValidationStatus.MINIMUM_MET, true, 0.7f)
            }
            wordCount < MIN_WORDS_OPTIMAL -> {
                Triple(ValidationStatus.RECOMMENDED, true, 0.85f)
            }
            else -> {
                Triple(ValidationStatus.OPTIMAL, true, 1.0f)
            }
        }

        // Generate user-friendly message
        val message = generateMessage(status, wordCount, charCount, sentenceCount)
        val recommendation = generateRecommendation(status, wordCount)

        Log.d(TAG, """
            Text Validation:
            - Word Count: $wordCount
            - Character Count: $charCount
            - Sentence Count: $sentenceCount
            - Status: $status
            - Confidence Multiplier: ${(confidenceMultiplier * 100).toInt()}%
            - Valid: $isValid
        """.trimIndent())

        return ValidationResult(
            isValid = isValid,
            status = status,
            message = message,
            recommendation = recommendation,
            wordCount = wordCount,
            charCount = charCount,
            sentenceCount = sentenceCount,
            confidenceMultiplier = confidenceMultiplier,
            needsMoreText = status in listOf(
                ValidationStatus.TOO_SHORT_ABSOLUTE,
                ValidationStatus.TOO_SHORT_LOW_CONFIDENCE,
                ValidationStatus.TOO_SHORT_ACCEPTABLE
            )
        )
    }

    /**
     * Enhanced validation with specific requirements
     */
    fun validateWithRequirements(
        text: String,
        minWords: Int = MIN_WORDS_RECOMMENDED,
        minChars: Int = MIN_CHARS_RECOMMENDED,
        requireSentences: Boolean = true
    ): EnhancedValidationResult {
        val baseValidation = validateText(text)

        val meetsWordRequirement = baseValidation.wordCount >= minWords
        val meetsCharRequirement = baseValidation.charCount >= minChars
        val meetsSentenceRequirement = if (requireSentences) {
            baseValidation.sentenceCount >= 2
        } else true

        val allRequirementsMet = meetsWordRequirement &&
                meetsCharRequirement &&
                meetsSentenceRequirement

        val requirements = mutableListOf<String>()
        if (!meetsWordRequirement) {
            requirements.add("• Add more words (need at least $minWords, currently ${baseValidation.wordCount})")
        }
        if (!meetsCharRequirement) {
            requirements.add("• Add more characters (need at least $minChars, currently ${baseValidation.charCount})")
        }
        if (!meetsSentenceRequirement && requireSentences) {
            requirements.add("• Add more sentences (need at least 2 sentences for better analysis)")
        }

        return EnhancedValidationResult(
            baseValidation = baseValidation,
            meetsWordRequirement = meetsWordRequirement,
            meetsCharRequirement = meetsCharRequirement,
            meetsSentenceRequirement = meetsSentenceRequirement,
            allRequirementsMet = allRequirementsMet,
            unmetRequirements = requirements,
            canProceedWithLowConfidence = baseValidation.wordCount >= MIN_WORDS_LOW_CONFIDENCE
        )
    }

    /**
     * Count words in text (handles multiple spaces, punctuation)
     */
    private fun countWords(text: String): Int {
        return text.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .size
    }

    /**
     * Count sentences (based on punctuation)
     */
    private fun countSentences(text: String): Int {
        return text.split(Regex("[.!?]"))
            .filter { it.isNotBlank() }
            .size
    }

    /**
     * Generate user-friendly message based on validation status
     */
    private fun generateMessage(
        status: ValidationStatus,
        wordCount: Int,
        charCount: Int,
        sentenceCount: Int
    ): String {
        return when (status) {
            ValidationStatus.EMPTY ->
                "⚠️ Empty Input\n\nPlease enter text to analyze."

            ValidationStatus.TOO_SHORT_ABSOLUTE ->
                buildString {
                    appendLine("❌ Text Too Short for Analysis")
                    appendLine()
                    appendLine("Your text contains only $wordCount word(s).")
                    appendLine("This is insufficient for AI detection.")
                    appendLine()
                    appendLine("📊 Why this matters:")
                    appendLine("• AI detection needs patterns in writing style")
                    appendLine("• Short text lacks statistical significance")
                    appendLine("• Results would be unreliable (<40% accuracy)")
                }

            ValidationStatus.TOO_SHORT_LOW_CONFIDENCE ->
                buildString {
                    appendLine("⚠️ Text Length Insufficient")
                    appendLine()
                    appendLine("Your text has $wordCount words ($charCount characters).")
                    appendLine("This is too short for reliable detection.")
                    appendLine()
                    appendLine("📈 Current Limitations:")
                    appendLine("• Accuracy would be only 55-65%")
                    appendLine("• High risk of false positives/negatives")
                    appendLine("• Cannot detect consistent patterns")
                }

            ValidationStatus.TOO_SHORT_ACCEPTABLE ->
                buildString {
                    appendLine("⚠️ Minimum Length Not Met")
                    appendLine()
                    appendLine("Your text has $wordCount words.")
                    appendLine("While analysis is possible, results will have limited reliability.")
                    appendLine()
                    appendLine("📊 Expected Accuracy: 65-75%")
                    appendLine("💡 For best results, aim for 100+ words.")
                }

            ValidationStatus.MINIMUM_MET ->
                buildString {
                    appendLine("✓ Minimum Requirements Met")
                    appendLine()
                    appendLine("Your text has $wordCount words.")
                    appendLine("Analysis can proceed with moderate confidence.")
                    appendLine()
                    appendLine("📊 Expected Accuracy: 75-85%")
                    appendLine("💡 For optimal results, aim for 200+ words.")
                }

            ValidationStatus.RECOMMENDED ->
                buildString {
                    appendLine("✓ Good Text Length")
                    appendLine()
                    appendLine("Your text has $wordCount words ($charCount characters).")
                    appendLine("This length is suitable for reliable detection.")
                    appendLine()
                    appendLine("📊 Expected Accuracy: 85-90%")
                    appendLine("✓ $sentenceCount sentences provide good pattern diversity")
                }

            ValidationStatus.OPTIMAL ->
                buildString {
                    appendLine("✓ Optimal Text Length")
                    appendLine()
                    appendLine("Excellent! Your text has $wordCount words.")
                    appendLine("This length provides maximum detection accuracy.")
                    appendLine()
                    appendLine("📊 Expected Accuracy: 90-95%")
                    appendLine("✓ Rich pattern data for reliable classification")
                }
        }
    }

    /**
     * Generate actionable recommendation
     */
    private fun generateRecommendation(
        status: ValidationStatus,
        currentWordCount: Int
    ): String {
        return when (status) {
            ValidationStatus.EMPTY ->
                "Enter any text to begin analysis"

            ValidationStatus.TOO_SHORT_ABSOLUTE ->
                buildString {
                    appendLine("📝 How to improve:")
                    appendLine("• Add at least ${MIN_WORDS_RECOMMENDED - currentWordCount} more words")
                    appendLine("• Include multiple sentences (2-3 or more)")
                    appendLine("• Write in complete paragraphs")
                    appendLine("• Provide context and details")
                }

            ValidationStatus.TOO_SHORT_LOW_CONFIDENCE ->
                buildString {
                    appendLine("📝 Suggestions to improve accuracy:")
                    appendLine("• Expand your text to 50+ words")
                    appendLine("• Add supporting details or examples")
                    appendLine("• Include multiple sentences with varied structure")
                    appendLine("• Provide context for better pattern detection")
                }

            ValidationStatus.TOO_SHORT_ACCEPTABLE ->
                buildString {
                    appendLine("💡 For more reliable results:")
                    appendLine("• Add ${MIN_WORDS_RECOMMENDED - currentWordCount} more words")
                    appendLine("• Include varied sentence structures")
                    appendLine("• Add examples or supporting details")
                }

            ValidationStatus.MINIMUM_MET ->
                buildString {
                    appendLine("💡 To improve further:")
                    appendLine("• Add ${MIN_WORDS_OPTIMAL - currentWordCount} more words for optimal accuracy")
                    appendLine("• Ensure text has natural variation")
                    appendLine("• Include personal voice if human-written")
                }

            else -> "Your text length is good. Proceed with analysis."
        }
    }

    /**
     * Get confidence adjustment factor based on text length
     */
    fun getConfidenceAdjustment(wordCount: Int): Float {
        return when {
            wordCount < MIN_WORDS_ACCEPTABLE -> 0.3f  // Reduce confidence by 70%
            wordCount < MIN_WORDS_RECOMMENDED -> 0.6f // Reduce confidence by 40%
            wordCount < MIN_WORDS_OPTIMAL -> 0.8f     // Reduce confidence by 20%
            else -> 1.0f                              // No reduction
        }
    }

    /**
     * Get user-friendly length indicator
     */
    fun getLengthIndicator(wordCount: Int): LengthIndicator {
        return when {
            wordCount < MIN_WORDS_ABSOLUTE -> LengthIndicator.VERY_POOR
            wordCount < MIN_WORDS_LOW_CONFIDENCE -> LengthIndicator.POOR
            wordCount < MIN_WORDS_ACCEPTABLE -> LengthIndicator.MINIMAL
            wordCount < MIN_WORDS_RECOMMENDED -> LengthIndicator.ACCEPTABLE
            wordCount < MIN_WORDS_OPTIMAL -> LengthIndicator.GOOD
            else -> LengthIndicator.EXCELLENT
        }
    }

    // Enums and Data Classes
    enum class ValidationStatus {
        EMPTY,
        TOO_SHORT_ABSOLUTE,
        TOO_SHORT_LOW_CONFIDENCE,
        TOO_SHORT_ACCEPTABLE,
        MINIMUM_MET,
        RECOMMENDED,
        OPTIMAL
    }

    enum class LengthIndicator {
        VERY_POOR,   // < 10 words
        POOR,        // 10-20 words
        MINIMAL,     // 20-50 words
        ACCEPTABLE,  // 50-100 words
        GOOD,        // 100-200 words
        EXCELLENT    // 200+ words
    }

    data class ValidationResult(
        val isValid: Boolean,
        val status: ValidationStatus,
        val message: String,
        val recommendation: String,
        val wordCount: Int,
        val charCount: Int,
        val sentenceCount: Int = 0,
        val confidenceMultiplier: Float,
        val needsMoreText: Boolean
    )

    data class EnhancedValidationResult(
        val baseValidation: ValidationResult,
        val meetsWordRequirement: Boolean,
        val meetsCharRequirement: Boolean,
        val meetsSentenceRequirement: Boolean,
        val allRequirementsMet: Boolean,
        val unmetRequirements: List<String>,
        val canProceedWithLowConfidence: Boolean
    )
}
