package net.swofty.catchngo.api

/**
 * All shared DTOs used by the Android client.
 *
 * KEEP THIS OBJECT UNIQUE: there must be exactly one `object ApiModels`
 * in your source-set, otherwise the compiler will silently pick one and
 * you’ll end up with “unresolved reference” errors like the one you saw.
 */
object ApiModels {

    /* ─────────────────────────────────────────────────────────────────── */
    /*  OAuth2 / authentication                                           */
    /* ─────────────────────────────────────────────────────────────────── */

    /** Bearer token returned by `/auth/login` (and sometimes `/register`) */
    data class TokenResponse(
        val accessToken: String,
        val tokenType: String
    )

    /** Result of `/auth/register` */
    data class RegisterResponse(
        val ok: Boolean,
        val reason:      String? = null          // e.g. "USERNAME_EXISTS"
    )

    /* ─────────────────────────────────────────────────────────────────── */
    /*  Registration-question models                                      */
    /* ─────────────────────────────────────────────────────────────────── */

    data class Question(val id: Int, val questionText: String)
    data class QuestionAnswer(val id: Int, val answer: String)

    /* ─────────────────────────────────────────────────────────────────── */
    /*  Nearby-player model                                               */
    /* ─────────────────────────────────────────────────────────────────── */

    /**
     * One player returned by `GET /location/nearby`.
     */
    data class NearbyUser(
        val id:            String,
        val name:          String,
        val points:        Int,
        val disabled:      Boolean,
        val questions:     List<QuestionAnswer>,
        val friends:       List<String>,   // list of *their* friend-IDs
        val selectedFriend:String?,        // whom they’re tracking
        val latitude:      Double,
        val longitude:     Double
    ) {
        /** Great-circle distance to another lat/lng, in metres. */
        fun distanceTo(otherLat: Double, otherLng: Double): Float {
            val res = FloatArray(1)
            android.location.Location.distanceBetween(
                latitude, longitude,
                otherLat,  otherLng,
                res
            )
            return res[0]
        }
    }

    /* ─────────────────────────────────────────────────────────────────── */
    /*  Leaderboard-related models                                        */
    /* ─────────────────────────────────────────────────────────────────── */

    data class LeaderboardUser(
        val id:     String,
        val name:   String,
        val points: Int
    )

    /* ─────────────────────────────────────────────────────────────────── */
    /*  Friendex models                                                   */
    /* ─────────────────────────────────────────────────────────────────── */

    /**
     * A player “card” shown in Friendex lists.
     */
    data class UserEntry(
        val id:        String,
        val name:      String,
        val points:    Int,
        val disabled:  Boolean,
        val questions: List<QuestionAnswer>
    )

    /* Add this to the ApiModels.kt file in the "Friendex models" section */

    /**
     * Status of currently selected user for tracking.
     * Returned by GET /friendex/select/check
     */
    data class SelectionStatus(
        val selectedFriend: String?,        // ID of the selected friend, null if none
        val isInitiator: Boolean,           // Whether this user initiated the tracking
        val timeRemaining: Double,          // Time remaining in seconds
        val elapsedTime: Double,            // Elapsed time in seconds
        val questionsReady: Boolean,        // Whether questions are ready (for quiz feature)
        val pointsAccumulated: Int          // Points accumulated so far in this tracking session
    )


    /**
     * Multiple-choice option model for MCQ questions.
     */
    data class McqOption(
        val id: Int,
        val answerText: String
    )

    /**
     * Multiple-choice question model with options.
     */
    data class McqQuestion(
        val id: Int,
        val questionText: String,
        val options: List<McqOption>
    )

    /**
     * Represents an achievement that the user has earned
     */
    data class Achievement(
        val title: String,
        val description: String,
        val points: Int
    )

    /**
     * Represents an achievement that the user hasn't earned yet
     */
    data class MissingAchievement(
        val title: String,
        val description: String,
        val points: Int,
        val minFriends: Int
    )
}
