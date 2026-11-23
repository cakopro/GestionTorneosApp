package com.mazecube.tournamentbracket

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.util.TypedValue
import android.view.ScaleGestureDetector
import kotlin.random.Random
import com.mazecube.tournamentbracket.data.BracketData
import com.mazecube.tournamentbracket.data.Match
import com.mazecube.tournamentbracket.data.MatchData
import com.mazecube.tournamentbracket.data.MatchParticipant
import android.view.ViewConfiguration
import kotlin.math.abs

class TournamentBracketView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    companion object {
        const val TOURNAMENT_TAG = "TOURNAMENT_TAG"
    }

    // Scale variables
    private var contentWidth = 0f
    private var contentHeight = 0f
    private var maxScaleFactor = 1.0f
    private var minScaleFactor = 0.5f
    private var scaleFactor = 1.0f
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var posX = 0f
    private var posY = 0f

    var onMatchClickListener: ((roundIndex: Int, matchIndex: Int) -> Unit)? = null

    var onThirdPlaceClickListener: (() -> Unit)? = null
    private var thirdPlaceMatch: Match? = null
    var thirdPlaceLabel: String = "Tercer lugar"

    // Tono bronce claro para distinguirlo del resto del bracket
    var thirdPlaceRectPaint: Paint = Paint().apply {
        color = Color.parseColor("#E6D5C3")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    var thirdPlaceLabelPaint: Paint = Paint().apply {
        color = Color.parseColor("#8D6E63")
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, resources.displayMetrics)
        isFakeBoldText = true
        isAntiAlias = true
    }
    private var downX = 0f
    private var downY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var huboMultitouch = false

    //Setting
    private var backgroundColor: Int = context.getColor(R.color.tournament_background)

    /**
     * Bracket
     */
    var bracketRoundRectPaint: Paint = Paint().apply {
        color = context.getColor(R.color.bracket_rect)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    var bracketRoundRectWidthPadding: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics)
    var bracketRoundRectHeightPadding: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics)
    var bracketRound: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics)
    var bracketRoundGapSize: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, resources.displayMetrics)
    var bracketTextPaint: Paint = Paint().apply {
        color = context.getColor(R.color.bracket_text)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
        isAntiAlias = true
    }
    var bracketImageVisible: Boolean = true
    var bracketTextMaxLength: Int = 20
    var bracket_bye_text: String = ""

    /**
     * Bracket Point
     */
    var bracketPointRectPaint: Paint = Paint().apply {
        color = context.getColor(R.color.bracket_point_rect)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    var bracketPointPaint: Paint = Paint().apply {
        color = context.getColor(R.color.bracket_text)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
        isAntiAlias = true
    }
    var bracketPointVisible: Boolean = true
    var bracketPointMaxLength: Int = 20

    /**
     * Line
     */
    var defaultLineColor: Int = Color.LTGRAY
    var winnerLineColor: Int = context.getColor(R.color.bracket_line_winner)
    var defaultLineWidth: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
    var winnerLineWidth: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)

    val bracketLinePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val bracketWinnerLinePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    /**
     * Other
     */
    var participantIsEmptyPaint: Paint = Paint().apply {
        color = Color.BLACK
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32f, resources.displayMetrics)
        isAntiAlias = true
    }

    //Match List
    private var matchCountPerRound: List<List<Match>> = mutableListOf()

    //Final Win
    private var finalWinner: BracketData? = null

    init {
        // Reset content dimensions
        contentWidth = 0f
        contentHeight = 0f

        applyAttribute(attrs)
    }

    @SuppressLint("CustomViewStyleable")
    private fun applyAttribute(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TournamentBracket, 0, 0)
        try {
            backgroundColor = typedArray.getColor(R.styleable.TournamentBracket_background_color, Color.GRAY)

            //Bracket
            bracketRoundRectPaint.color = typedArray.getColor(R.styleable.TournamentBracket_bracket_default_color, Color.WHITE)
            bracketRoundRectWidthPadding = typedArray.getDimension(R.styleable.TournamentBracket_bracket_width_padding,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics))
            bracketRoundRectHeightPadding = typedArray.getDimension(R.styleable.TournamentBracket_bracket_height_padding,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics))
            bracketRound = typedArray.getDimension(R.styleable.TournamentBracket_bracket_round,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics))
            bracketRoundGapSize = typedArray.getDimension(R.styleable.TournamentBracket_bracket_round_gap_size,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200f, resources.displayMetrics))
            bracketTextPaint.textSize = typedArray.getDimension(R.styleable.TournamentBracket_bracket_text_size,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics))
            bracketImageVisible = typedArray.getBoolean(R.styleable.TournamentBracket_bracket_image_visible, true)
            bracketTextMaxLength = typedArray.getInt(R.styleable.TournamentBracket_bracket_text_max_length, 20)
            bracket_bye_text = typedArray.getString(R.styleable.TournamentBracket_bracket_bye_text) ?: ""

            //Point
            bracketPointPaint.textSize = typedArray.getDimension(R.styleable.TournamentBracket_bracket_point_text_size,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics))
            bracketPointVisible = typedArray.getBoolean(R.styleable.TournamentBracket_bracket_point_text_visible, true)
            bracketPointMaxLength = typedArray.getInt(R.styleable.TournamentBracket_bracket_point_text_max_length, 20)

            defaultLineColor = typedArray.getColor(R.styleable.TournamentBracket_default_line_color, Color.LTGRAY)
            winnerLineColor = typedArray.getColor(R.styleable.TournamentBracket_winner_line_color, Color.GREEN)
            defaultLineWidth = typedArray.getFloat(R.styleable.TournamentBracket_default_line_width, 2f)
            winnerLineWidth = typedArray.getFloat(R.styleable.TournamentBracket_winner_line_width, 2f)
            initLine()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            typedArray.recycle()
        }
    }

    private fun initLine() {
        bracketLinePaint.color = defaultLineColor
        bracketLinePaint.strokeWidth = defaultLineWidth
        bracketWinnerLinePaint.color = winnerLineColor
        bracketWinnerLinePaint.strokeWidth = winnerLineWidth
    }

    /**
     * Sets the maximum size scale for the tournament.
     * This function calculates the maximum width and height required to display the entire tournament,
     * including the final winner, by iterating through each match and bracket in the tournament.
     * It adjusts the contentWidth and contentHeight based on the right and bottom coordinates of each bracket's rectangle,
     * and adds padding for better spacing.
     *
     * @param tournament A list of rounds, each containing a list of matches, where each match includes two brackets.
     */
    private fun setMaxSizeScale(tournament: List<List<Match>>) {
        var maxRight = 0f
        var maxBottom = 0f
        fun medir(r: RectF?) {
            if (r != null) {
                maxRight = maxOf(maxRight, r.right)
                maxBottom = maxOf(maxBottom, r.bottom)
            }
        }
        tournament.forEach { ronda ->
            ronda.forEach { match ->
                medir(match.bracketOne.rect)
                medir(match.bracketTwo.rect)
            }
        }
        medir(finalWinner?.rect)

        contentWidth = maxRight + bracketRoundRectWidthPadding
        contentHeight = maxBottom + bracketRoundRectHeightPadding
    }

    /**
     * Generates a tournament bracket structure based on the given participants.
     *
     * @param players List of participants.
     * @param numPlayers Total number of participants.
     * @param isShuffle Flag to indicate if participants should be shuffled.
     * @return A list of rounds, each containing a list of matches.
     */
    private fun generateTournament(players: List<MatchParticipant>, numPlayers: Int, isShuffle: Boolean = false): List<List<Match>> {
        val closestPowerOfTwo = findClosestPowerOfTwo(numPlayers)
        val byesNeeded = closestPowerOfTwo - numPlayers
        val shuffledPlayers = if (isShuffle) players.shuffled(Random(System.currentTimeMillis())) else players
        val tournament = mutableListOf<List<Match>>()

        // Round 1 matches
        val round1Matches = mutableListOf<Match>()
        for (i in 0 until closestPowerOfTwo step 2) {
            round1Matches.add(if (round1Matches.size < byesNeeded) createMatch(true) else createMatch(false))
        }

        // Separate matches into bye and regular matches
        val (byeMatches, regularMatches) = round1Matches.partition { it.bracketTwo.isBye }
        val finalSortedRound1Matches = (regularMatches + byeMatches).toMutableList()

        // Initialize positions
        var initX = 20f
        var initY = 20f
        val roundOneMatches = finalSortedRound1Matches.mapIndexed { index, match ->
            val one = getBracketReData(initX, initY, match.bracketOne)
            initY += one.height ?: 0f
            val two = getBracketReData(initX, initY, match.bracketTwo)
            initY += if (!two.isBye) ((one.height ?: 1f) * 2) else one.height ?: 0f
            Match(one, two)
        }.toMutableList()

        // Assign participants to Round 1 matches
        var currentPlayerIndex = 0
        roundOneMatches.forEach { match ->
            if (!match.bracketOne.isBye) match.bracketOne.participant = shuffledPlayers[currentPlayerIndex++]
            if (!match.bracketTwo.isBye) match.bracketTwo.participant = shuffledPlayers[currentPlayerIndex++]
        }

        tournament.add(roundOneMatches)

        // Generate subsequent rounds
        generateSubsequentRounds(tournament, roundOneMatches.size / 2)

        // Set final winner position
        setFinalWinnerPosition(tournament)

        return tournament
    }

    private fun createMatch(isBye: Boolean) = Match(
        BracketData(participant = null, isBye = false),
        BracketData(participant = null, isBye = isBye)
    )

    private fun generateSubsequentRounds(tournament: MutableList<List<Match>>, initialRemainingMatches: Int) {
        var remainingMatches = initialRemainingMatches
        var initX = 20f

        while (remainingMatches > 0) {
            val roundMatches = mutableListOf<Match>()
            initX = (tournament.last().first().bracketOne.rect?.right ?: 20f) + bracketRoundGapSize
            var beforeBracketDataCount = 0

            for (i in 1..remainingMatches) {
                val beforeBracketData = if (!tournament.last()[beforeBracketDataCount + 1].bracketTwo.isBye) {
                    tournament.last()[beforeBracketDataCount + 1].bracketTwo
                } else {
                    tournament.last()[beforeBracketDataCount + 1].bracketOne
                }
                val initY = positionRectAtCenter(tournament.last()[beforeBracketDataCount].bracketOne.rect!!, beforeBracketData.rect!!)
                val one = getBracketReData(initX, initY, BracketData(participant = null, isBye = false))
                val two = getBracketReData(initX, initY + (one.height ?: 0f), BracketData(participant = null, isBye = false))

                roundMatches.add(Match(one, two))
                beforeBracketDataCount += 2
            }

            tournament.add(roundMatches)
            remainingMatches /= 2
        }
    }

    private fun setFinalWinnerPosition(tournament: List<List<Match>>) {
        val lastMatch = tournament.last().last()
        val finalInitX = (lastMatch.bracketOne.rect?.right ?: 20f) + (bracketRoundGapSize / 2)
        val finalInitY = positionRectAtCenter(lastMatch.bracketOne.rect!!, lastMatch.bracketTwo.rect!!)
        finalWinner = initFinalBracketData(finalInitX, finalInitY, BracketData(participant = null, isBye = false))
    }


    /**
     * Finds the closest power of two greater than or equal to the given number.
     *
     * @param num The input number.
     * @return The closest power of two greater than or equal to num.
     */
    fun findClosestPowerOfTwo(num: Int): Int {
        var power = 1
        while (power < num) {
            power *= 2
        }
        return power
    }

    /**
     * Generates bracket data with calculated positions and sizes for a given participant's position.
     *
     * @param posX The x-coordinate for the bracket's position.
     * @param posY The y-coordinate for the bracket's position.
     * @param bracketData The initial bracket data.
     * @return The updated BracketData with calculated positions and sizes.
     */
    private fun getBracketReData(posX: Float, posY: Float, bracketData: BracketData): BracketData {
        // Measure text width and height (Nickname)
        val textWidth = bracketTextPaint.measureText(getTextSizeMaxLength(bracketTextMaxLength))
        val fontMetrics = bracketTextPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val pointWidth = bracketPointPaint.measureText(getTextSizeMaxLength(bracketPointMaxLength))
        val bitmapSize = if (bracketImageVisible) textHeight else 0f

        // Adjust rect size based on text size
        val rect = RectF(
            posX, posY,
            posX + (textWidth + pointWidth) + bitmapSize + (if (bracketImageVisible) 3 else 2) * bracketRoundRectWidthPadding,
            posY + textHeight + 2 * bracketRoundRectHeightPadding
        )

        // Calculate bitmap position
        val bitmapXPos = if (bracketImageVisible) posX + bracketRoundRectWidthPadding else null
        val bitmapYPos = if (bracketImageVisible) posY + bracketRoundRectHeightPadding else null

        // Calculate text position
        val xPos = posX + bitmapSize + (if (bracketImageVisible) 2 else 1) * bracketRoundRectWidthPadding
        val yPos = posY + bracketRoundRectHeightPadding - fontMetrics.ascent

        return BracketData(
            participant = bracketData.participant,
            isBye = bracketData.isBye,
            bitmapSize = bitmapSize.toInt(),
            bitmapStartX = bitmapXPos,
            bitmapStartY = bitmapYPos,
            textX = xPos,
            textY = yPos,
            rect = rect,
            width = rect.width(),
            height = rect.height(),
            widthPadding = bracketRoundRectWidthPadding,
            heightPadding = bracketRoundRectHeightPadding,
            round = bracketRound
        )
    }

    /**
     * Initializes the final bracket data with calculated positions and sizes for the final winner.
     *
     * @param posX The x-coordinate for the bracket's position.
     * @param posY The y-coordinate for the bracket's position.
     * @param bracketData The initial bracket data.
     * @return The updated BracketData with calculated positions and sizes for the final winner.
     */
    private fun initFinalBracketData(posX: Float, posY: Float, bracketData: BracketData): BracketData {
        // Measure text width and height (Nickname)
        val textWidth = bracketTextPaint.measureText(bracketData.participant?.nickname ?: getTextSizeMaxLength(bracketTextMaxLength))
        val fontMetrics = bracketTextPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val bitmapSize = if (bracketImageVisible) textHeight * 3 else 0f

        // Adjust rect size based on text size
        val rectWidth = maxOf(bitmapSize, textWidth) + 2 * bracketRoundRectWidthPadding
        val rectHeight = bitmapSize + textHeight + bracketRoundRectHeightPadding
        val rect = RectF(posX, posY, posX + rectWidth, posY + rectHeight)

        // Calculate positions
        val bitmapXPos = posX + (rectWidth - bitmapSize) / 2
        val bitmapYPos = posY + bracketRoundRectHeightPadding
        val textXPos = posX + (rectWidth - textWidth) / 2
        val textYPos = bitmapYPos + bitmapSize - fontMetrics.ascent // ascent is negative

        return BracketData(
            participant = bracketData.participant,
            isBye = bracketData.isBye,
            bitmapSize = bitmapSize.toInt(),
            bitmapStartX = bitmapXPos,
            bitmapStartY = bitmapYPos,
            textX = textXPos,
            textY = textYPos,
            rect = rect,
            width = rect.width(),
            height = rect.height(),
            widthPadding = bracketRoundRectWidthPadding,
            heightPadding = bracketRoundRectHeightPadding,
            round = bracketRound
        )
    }


    /**
     * Positions rect2 at the vertical center of rect1.
     *
     * @param rect1 The first rectangle.
     * @param rect2 The second rectangle.
     * @return The y-coordinate to position rect2 at the vertical center of rect1.
     */
    private fun positionRectAtCenter(rect1: RectF, rect2: RectF): Float {
        return (rect1.top + rect2.top) / 2 - rect1.height() / 2
    }

    /**
     * Generates a string of 'A' characters of the specified maximum length.
     *
     * @param maxLength The maximum length of the string.
     * @return A string of 'A' characters.
     */
    private fun getTextSizeMaxLength(maxLength: Int): String {
        return "A".repeat(maxLength)
    }

    /**
     * Truncates the given text to the specified maximum length, adding "..." if truncated.
     *
     * @param text The text to be truncated.
     * @param maxLength The maximum length of the text.
     * @return The truncated text.
     */
    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) text.take(maxLength) + "..." else text
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw the background color
        canvas.drawColor(backgroundColor)

        // Save the current canvas state
        canvas.save()

        // Apply scaling and translation based on the current scale factor and position
        canvas.scale(scaleFactor, scaleFactor)
        canvas.translate(posX / scaleFactor, posY / scaleFactor)

        if (matchCountPerRound.isEmpty()) {
            // If there are no participants, display a message
            canvas.drawText(
                "Participant is empty.",
                bracketRoundRectWidthPadding + 100f,
                bracketRoundRectHeightPadding + 100f,
                participantIsEmptyPaint
            )
        } else {
            // Draw the tournament matches
            matchCountPerRound.forEachIndexed { roundIndex, matches ->
                matches.forEachIndexed { matchIndex, match ->
                    // Draw the participants in each match
                    drawPlayer(canvas, match.bracketOne)
                    drawPlayer(canvas, match.bracketTwo)

                    // Draw the lines connecting the matches
                    val nextMatch = matchCountPerRound.getOrNull(roundIndex + 1)?.getOrNull(matchIndex / 2)
                    drawLine(canvas, match, nextMatch)
                }
            }

            // Draw the final winner if available
            finalWinner?.let {
                drawFinalWinner(canvas, it)
            }
            thirdPlaceMatch?.let { drawThirdPlace(canvas, it) }
        }

        // Restore the canvas to its previous state
        canvas.restore()
    }

    /**
     * Draws the line connecting the current match to the next match.
     *
     * @param canvas The canvas to draw on.
     * @param match The current match.
     * @param nextMatch The next match in the tournament bracket.
     */
    private fun drawLine(canvas: Canvas, match: Match, nextMatch: Match?) {
        if (!match.bracketTwo.isBye) {
            drawPath(canvas, match.bracketOne, nextMatch, isBracketOne = true)
            drawPath(canvas, match.bracketTwo, nextMatch, isBracketOne = false)
        } else {
            drawPathForBye(canvas, match.bracketOne, nextMatch)
        }
    }

    /**
     * Draws the path for a participant from the current match to the next match.
     *
     * @param canvas The canvas to draw on.
     * @param bracket The current bracket data.
     * @param nextMatch The next match in the tournament bracket.
     * @param isBracketOne Boolean flag indicating if this is the first bracket.
     */
    private fun drawPath(canvas: Canvas, bracket: BracketData, nextMatch: Match?, isBracketOne: Boolean) {
        val path = Path()
        val (startY, endY) = if (isBracketOne) {
            bracket.rect!!.top + bracket.height!! / 2 to bracket.rect.bottom
        } else {
            bracket.rect!!.top + bracket.height!! / 2 to bracket.rect.top
        }
        val midX = bracket.rect.right + bracketRoundGapSize / 4
        val endX = bracket.rect.right + bracketRoundGapSize / 2
        val nextY = nextMatch?.bracketOne?.rect?.bottom ?: 0f

        path.moveTo(bracket.rect.right, startY)
        path.lineTo(midX, startY)
        path.lineTo(midX, endY)
        if (bracket.participant?.isWinner != false) {
            path.lineTo(endX, endY)
            if (nextMatch != null) {
                path.lineTo(endX, nextY)
                path.lineTo(nextMatch.bracketOne.rect!!.left, nextY)
            }
        }

        canvas.drawPath(path, if (bracket.participant?.isWinner == true) bracketWinnerLinePaint else bracketLinePaint)
    }

    /**
     * Draws the path for a participant who has a bye to the next match.
     *
     * @param canvas The canvas to draw on.
     * @param bracket The current bracket data.
     * @param nextMatch The next match in the tournament bracket.
     */
    private fun drawPathForBye(canvas: Canvas, bracket: BracketData, nextMatch: Match?) {
        val path = Path()
        val midX = bracket.rect!!.right + bracketRoundGapSize / 2
        val nextY = nextMatch?.bracketOne?.rect?.bottom ?: 0f

        path.moveTo(bracket.rect.right, bracket.rect.top + bracket.height!! / 2)
        path.lineTo(midX, bracket.rect.top + bracket.height / 2)

        if (nextMatch != null) {
            path.lineTo(midX, nextY)
            path.lineTo(nextMatch.bracketOne.rect!!.left, nextY)
        }
        canvas.drawPath(path, if (bracket.participant?.isWinner == true) bracketWinnerLinePaint else bracketLinePaint)
    }

    /**
     * Draws a participant's bracket.
     *
     * @param canvas The canvas to draw on.
     * @param bracketData The bracket data for the participant.drawRoundRect
     */
    private fun drawPlayer(canvas: Canvas, bracketData: BracketData, rectPaint: Paint = bracketRoundRectPaint) {
        if (bracketData.isBye) return

        bracketData.rect?.let {
            canvas.drawRoundRect(it, bracketData.round ?: 0f, bracketData.round ?: 0f, rectPaint)
        }

        if (bracketPointVisible) {
            drawBracketPoint(canvas, bracketData)
        }

        bracketData.participant?.face?.let {
            if (bracketImageVisible) {
                drawParticipantFace(canvas, bracketData)
            }
        }

        canvas.drawText(
            truncateText(bracketData.participant?.nickname ?: bracket_bye_text, bracketTextMaxLength),
            bracketData.textX ?: 0f,
            bracketData.textY ?: 0f,
            bracketTextPaint
        )
    }

    /**
     * Draws the point for a participant's bracket.
     *
     * @param canvas The canvas to draw on.
     * @param bracketData The bracket data for the participant.
     */
    private fun drawBracketPoint(canvas: Canvas, bracketData: BracketData) {
        bracketData.rect?.let {
            val pointWidth = bracketPointPaint.measureText(getTextSizeMaxLength(bracketPointMaxLength))
            val pointStartX = it.right - pointWidth
            val pointRect = RectF(pointStartX, it.top, it.right, it.bottom)
            canvas.drawRoundRect(pointRect, bracketData.round ?: 0f, bracketData.round ?: 0f, bracketPointRectPaint)

            val text = truncateText(bracketData.participant?.point ?: "", bracketPointMaxLength)
            val textBounds = Rect()
            bracketTextPaint.getTextBounds(text, 0, text.length, textBounds)

            val textX = pointRect.left + (pointRect.width() - textBounds.width()) / 2
            val textY = pointRect.top + (pointRect.height() - textBounds.height()) / 2 + textBounds.height()

            canvas.drawText(text, textX, textY, bracketTextPaint)
        }
    }

    /**
     * Draws the participant's face in the bracket.
     *
     * @param canvas The canvas to draw on.
     * @param bracketData The bracket data for the participant.
     */

    /**
     * Dibuja un partido por el tercer lugar debajo de la final.
     * Se llama después de initParticipant. Con null en los dos jugadores muestra los recuadros vacíos.
     */
    fun setThirdPlace(playerA: MatchParticipant?, playerB: MatchParticipant?) {
        val finalMatch = matchCountPerRound.lastOrNull()?.lastOrNull() ?: return
        val finalRect = finalMatch.bracketOne.rect ?: return
        val finalBottom = finalMatch.bracketTwo.rect?.bottom ?: finalRect.bottom

        val x = finalRect.left
        val y = finalBottom + bracketRoundRectHeightPadding * 3      // deja espacio para la etiqueta
        val one = getBracketReData(x, y, BracketData(participant = playerA, isBye = false))
        val two = getBracketReData(x, y + (one.height ?: 0f), BracketData(participant = playerB, isBye = false))
        thirdPlaceMatch = Match(one, two)

        contentHeight = maxOf(contentHeight, (two.rect?.bottom ?: 0f) + bracketRoundRectHeightPadding)
        if (width > 0 && height > 0) {
            calculateScaleFactor()
            limitDrag()
        }
        invalidate()
    }

    private fun drawThirdPlace(canvas: Canvas, match: Match) {
        drawPlayer(canvas, match.bracketOne, thirdPlaceRectPaint)
        drawPlayer(canvas, match.bracketTwo, thirdPlaceRectPaint)
        match.bracketOne.rect?.let {
            canvas.drawText(thirdPlaceLabel, it.left, it.top - bracketRoundRectHeightPadding, thirdPlaceLabelPaint)
        }
    }

    private fun drawParticipantFace(canvas: Canvas, bracketData: BracketData) {
        bracketData.bitmapStartX?.let { startX ->
            bracketData.bitmapStartY?.let { startY ->
                canvas.drawBitmap(
                    getCircularBitmap(
                        Bitmap.createScaledBitmap(
                            bracketData.participant!!.face!!,
                            bracketData.bitmapSize!!, bracketData.bitmapSize, true
                        )
                    ),
                    startX,
                    startY,
                    null
                )
            }
        }
    }

    /**
     * Draws the final winner's bracket.
     *
     * @param canvas The canvas to draw on.
     * @param bracketData The bracket data for the final winner.
     */
    private fun drawFinalWinner(canvas: Canvas, bracketData: BracketData) {
        bracketData.rect?.let {
            canvas.drawRoundRect(it, bracketData.round ?: 0f, bracketData.round ?: 0f, bracketRoundRectPaint)
        }

        bracketData.participant?.face?.let {
            if (bracketImageVisible) {
                drawParticipantFace(canvas, bracketData)
            }
        }

        canvas.drawText(
            truncateText(bracketData.participant?.nickname ?: bracket_bye_text, bracketTextMaxLength),
            bracketData.textX ?: 0f,
            bracketData.textY ?: 0f,
            bracketTextPaint
        )
    }

    /**
     * Called when the size of the view changes.
     *
     * @param w The new width of the view.
     * @param h The new height of the view.
     * @param oldw The old width of the view.
     * @param oldh The old height of the view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateScaleFactor()
        invalidate() // Redraw the view with the new scale factor
    }

    /**
     * Calculates the scale factor for the view based on its content size.
     */
    private fun calculateScaleFactor() {
        if (contentWidth > 0 && contentHeight > 0) {
            val widthScale = width / contentWidth
            val heightScale = height / contentHeight
            scaleFactor = minOf(widthScale, heightScale)
            maxScaleFactor = 2.0f // Set an appropriate maximum scale factor
            minScaleFactor = scaleFactor
        }
    }

    /**
     * Initializes the participants and generates the tournament bracket.
     *
     * This function sets up the tournament with the given participants. If `isShuffle` is true,
     * participants are shuffled before creating the matches; otherwise, they are added in order.
     * The function then generates the tournament bracket, scales the view accordingly, and triggers a redraw.
     *
     * @param participants List of participants.
     * @param isShuffle Flag to indicate if the participants should be shuffled. If true, participants are added randomly; if false, they are added in order. Default is false.
     * @return A list of MatchData representing the matches for the first round of the tournament.
     */
    fun initParticipant(participants: List<MatchParticipant>, isShuffle: Boolean = false): List<MatchData> {
        val numPlayers = participants.size
        val tournament = generateTournament(participants, numPlayers, isShuffle)

        setMaxSizeScale(tournament)
        matchCountPerRound = tournament
        thirdPlaceMatch = null

        posX = 0f
        posY = 0f
        if (width > 0 && height > 0) calculateScaleFactor()

        invalidate() // Redraw the view with the new tournament

        return mapMatchesToMatchData(tournament.first())
    }

    private fun mapMatchesToMatchData(matches: List<Match>): List<MatchData> {
        return matches.map { match ->
            MatchData(
                playerA = match.bracketOne.participant ?: throw NullPointerException("participant is null"),
                playerB = match.bracketTwo.participant
            )
        }
    }


    /**
     * Updates the match data with the latest participants' information and prepares the next round.
     *
     * Iterates through the current round's matches and updates the participants' information.
     * If a match is a bye or a participant has won, they are advanced to the next round.
     * If the final winner is determined, the winner is initialized.
     *
     * If the final winner is already determined, the function returns an empty list.
     *
     * @param participants List of participants with updated information.
     * @return List of MatchData representing the updated matches for the current round.
     */
    fun updateMatchData(participants: List<MatchParticipant>): List<MatchData> {
        if(finalWinner?.participant != null) return emptyList()

        var nextRound = 0
        outer@ for ((index, matches) in matchCountPerRound.withIndex()) {
            if (matches.first().bracketOne.participant?.isWinner == null) {
                Log.i(TOURNAMENT_TAG, "Round $index")
                nextRound = index

                for (match in matches) {
                    participants.forEach { participant ->
                        match.bracketOne.participant?.let { bracketOneParticipant ->
                            if (bracketOneParticipant.id == participant.id && bracketOneParticipant.nickname == participant.nickname) {
                                match.bracketOne.participant = participant
                            }
                        }
                        match.bracketTwo.participant?.let { bracketTwoParticipant ->
                            if (bracketTwoParticipant.id == participant.id && bracketTwoParticipant.nickname == participant.nickname) {
                                match.bracketTwo.participant = participant
                            }
                        }
                    }
                }
                break@outer
            }
        }

        val updatedParticipants = mutableListOf<MatchParticipant>()

        if (nextRound < matchCountPerRound.size && nextRound >= 1) {
            val nextRoundMatches = matchCountPerRound[nextRound]
            var matchIndex = 0
            matchCountPerRound[nextRound - 1].forEach { match ->
                var isNextMatch = false
                if (match.bracketTwo.isBye) {
                    match.bracketOne.participant?.let { isNextMatch = updateNextRoundParticipants(it, nextRoundMatches, matchIndex, updatedParticipants) }
                } else {
                    match.bracketOne.participant?.takeIf { it.isWinner == true }?.let { isNextMatch = updateNextRoundParticipants(it, nextRoundMatches, matchIndex, updatedParticipants) }
                    match.bracketTwo.participant?.takeIf { it.isWinner == true }?.let { isNextMatch = updateNextRoundParticipants(it, nextRoundMatches, matchIndex, updatedParticipants) }
                }

                if(isNextMatch) matchIndex++
            }
        } else {
            participants.find { it.isWinner == true }?.let { winner ->
                Log.i(TOURNAMENT_TAG, "Winner $winner")
                initFinalWinner(winner)

                invalidate()
                return emptyList()
            }
        }

        invalidate()
        return mapMatchesToMatchData(matchCountPerRound[nextRound])
    }

    /**
     * Retrieves the list of matches currently in progress.
     *
     * Iterates through the match count per round to find the first round where the winner
     * of any match has not been determined. If the isWinner variable is not set,
     * that round is considered to be currently in progress. Returns the list of matches for that round.
     *
     * @return A list of matches currently in progress. Returns an empty list if no matches are in progress.
     */
    fun getCurrentMatchListData(): List<Match> {
        for ((_, matches) in matchCountPerRound.withIndex()) {
            for (match in matches) {
                if (match.bracketOne.participant?.isWinner == null) {
                    return matches
                }
            }
        }

        return emptyList()
    }

    /**
    * Updates the participants for the next round and checks if the match is ready to proceed.
    *
    * @param participant The participant to update.
    * @param nextRoundMatches The matches in the next round.
    * @param matchIndex The index of the current match.
    * @param updatedParticipants The list of updated participants.
    * @return True if both participants for the next match are set, indicating the match is ready to proceed; false otherwise.
    */
    private fun updateNextRoundParticipants(participant: MatchParticipant, nextRoundMatches: List<Match>, matchIndex: Int, updatedParticipants: MutableList<MatchParticipant>): Boolean {
        var isNextMatch = false
        val updatedParticipant = participant.copy(isWinner = null, point = null)
        if (matchIndex < nextRoundMatches.size) {
            nextRoundMatches[matchIndex].apply {
                if (bracketOne.participant == null) {
                    bracketOne.participant = updatedParticipant
                } else if (bracketTwo.participant == null) {
                    bracketTwo.participant = updatedParticipant
                }
            }

            isNextMatch =
                nextRoundMatches[matchIndex].bracketOne.participant != null &&
                    nextRoundMatches[matchIndex].bracketTwo.participant != null
        }
        updatedParticipants.add(updatedParticipant)

        return isNextMatch
    }

    /**
     * Sets the final winner in the tournament bracket.
     *
     * @param winner The final winner.
     */
    fun initFinalWinner(winner: MatchParticipant) {
        finalWinner?.let {
            it.participant = winner
            invalidate()
        }
    }

    /**
     * Transforms a bitmap into a circular bitmap.
     *
     * @param bitmap The original bitmap.
     * @return The circular bitmap.
     */
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)

        paint.isAntiAlias = true
        paint.color = Color.BLACK

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(
            bitmap.width / 2f,
            bitmap.height / 2f,
            bitmap.width / 2f,
            paint
        )

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (event.pointerCount > 1) huboMultitouch = true          // NUEVO

        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                downX = event.x
                downY = event.y
                huboMultitouch = false                              // NUEVO
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                posX += dx
                posY += dy
                lastTouchX = event.x
                lastTouchY = event.y
                limitDrag()
                invalidate()
            }
            android.view.MotionEvent.ACTION_UP -> {
                val fueToque = abs(event.x - downX) < touchSlop && abs(event.y - downY) < touchSlop
                if (fueToque && !huboMultitouch && !scaleGestureDetector.isInProgress) {   // CAMBIADO
                    findMatchAt(event.x, event.y)?.let { (r, m) ->
                        if (r == -1) onThirdPlaceClickListener?.invoke()
                        else onMatchClickListener?.invoke(r, m)
                    }
                }
            }
        }
        return true
    }

    private fun findMatchAt(x: Float, y: Float): Pair<Int, Int>? {
        val cx = (x - posX) / scaleFactor
        val cy = (y - posY) / scaleFactor
        matchCountPerRound.forEachIndexed { r, matches ->
            matches.forEachIndexed { m, match ->
                val hit = match.bracketOne.rect?.contains(cx, cy) == true ||
                        (!match.bracketTwo.isBye && match.bracketTwo.rect?.contains(cx, cy) == true)
                if (hit) return Pair(r, m)
            }
        }

        thirdPlaceMatch?.let { t ->
            val hit = t.bracketOne.rect?.contains(cx, cy) == true || t.bracketTwo.rect?.contains(cx, cy) == true
            if (hit) return Pair(-1, 0)
        }
        return null

    }

    /**
     * Limits the drag to prevent content from moving out of view bounds.
     */
    private fun limitDrag() {
        val maxX = (width - contentWidth * scaleFactor).coerceAtMost(0f)
        val maxY = (height - contentHeight * scaleFactor).coerceAtMost(0f)
        posX = posX.coerceIn(maxX, 0f)
        posY = posY.coerceIn(maxY, 0f)
    }

    /**
     * Listener for scale gestures to handle zooming.
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScaleFactor, maxScaleFactor)
            invalidate()
            return true
        }
    }
}