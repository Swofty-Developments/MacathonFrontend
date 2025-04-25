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
}
