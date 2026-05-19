package com.oop.game.example

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.oop.game.GameWorld
import com.oop.game.InputHandler
import kotlin.math.floor
import kotlin.random.Random

/**
 * ════════════════════════════════════════════════════════════
 *  게임 월드 예제 — Player vs Enemy 회피 게임 (이미지 사용).
 * ════════════════════════════════════════════════════════════
 *
 *  GameWorld 를 상속해 만든 가장 작은 플레이 가능한 예제.
 *  학생은 이 파일을 참고해서 자기만의 월드를 만들면 된다.
 *
 *  ── 조작법 ──
 *   ▸ 화살표 키  : 플레이어 이동
 *   ▸ WASD      : 카메라 이동 (월드가 화면보다 커서 탐험 가능)
 *   ▸ ESC       : 게임 오버 후 종료
 *
 *  ── 사용 이미지 (core/src/main/resources/) ──
 *   ▸ player.png  — 30x30 플레이어 스프라이트
 *   ▸ enemy.png   — 40x40 적 스프라이트
 *   ▸ tile.png    — 64x64 흰색 정사각형 (체스판 배경에 색만 입혀 사용)
 *
 *  ── 게임 상태 ──
 *   IN_PLAY   : 일반 진행 (이동·충돌 체크)
 *   GAME_OVER : 충돌 후 정지, ESC 입력 대기
 *
 *  ── 텍스트 데모 ──
 *   ▸ 좌측 상단 "HP: 3"       — 화면 좌표 (카메라 움직여도 고정)
 *   ▸ 월드 중앙 "WORLD CENTER" — 월드 좌표 (카메라와 함께 이동)
 *   두 개를 같이 두어, 두 좌표계의 차이를 눈으로 확인할 수 있게 했다.
 *
 *  ── 배경 ──
 *   tile.png(흰 사각형)를 두 가지 색으로 틴트해 체스판처럼 깐다.
 *   카메라 이동을 눈으로 보여주기 위함이다.
 *   GameWorld.drawBackground(batch) 를 override 해서 그린다.
 *
 * @param screenWidth  화면에 보이는 영역 너비
 * @param screenHeight 화면에 보이는 영역 높이
 * @param worldWidth   월드 전체 너비 (화면보다 크면 WASD 로 탐험 가능)
 * @param worldHeight  월드 전체 높이
 */
class ExampleWorld(
    screenWidth: Float,
    screenHeight: Float,
    worldWidth: Float,
    worldHeight: Float
) : GameWorld(screenWidth, screenHeight, worldWidth, worldHeight) {

    /**
     * 게임의 현재 상태를 나타내는 열거형.
     *
     * Boolean 깃발(isGameOver) 대신 enum 을 쓰는 이유:
     *   ▸ 상태 가짓수가 늘어날 때 깔끔히 확장 가능 (예: PAUSED, MENU, VICTORY)
     *   ▸ when 으로 분기하면 'else' 없이 모든 상태를 다뤘는지 컴파일러가 체크해줌
     *   ▸ 코드를 읽을 때 "이 게임에 어떤 상태들이 있는가" 가 한눈에 보임
     *   (7주차에서 배우는 enum class 의 전형적 활용)
     */
    private enum class GameState {
        IN_PLAY,
        GAME_OVER
    }
    //모든 캐릭터들을 여러개 담을 리스트
    private val players = mutableListOf<ExampleEnemy>()
    //캐릭터의 이미지를 한 묶음으로 담을 데이터 통
    private data class ChrData(val texture: Texture)
    private val playerTextures = listOf(
        ChrData(Texture(Gdx.files.internal("char1.png"))),
        ChrData(Texture(Gdx.files.internal("char1-debuff.png"))),
        ChrData(Texture(Gdx.files.internal("char1-buff.png")))
    )

    // 플레이어 — 월드 중앙 하단에서 시작.
    //   월드 크기를 함께 넘겨서, 경계 밖으로 못 나가게 한다.
    private val player = ExamplePlayer(
        x = worldWidth / 2 - 15f,   // 가로 30 의 절반을 빼서 정확히 중앙
        y = 50f,
        worldWidth = worldWidth,
        worldHeight = worldHeight,
        texture = playerTextures[0].texture
    )
    private var spawnTimer = 0f         // 시간이 얼마나 지났는지 기록할 변수
    private val spawnInterval = 1f    // 음식이 떨어지는 간격 (1초마다)

    private var rareSpawnTimer = 0f //희귀 음식 시간을 기록할 변수
    private val rareSpawnInterval = 10f

    private var debuffSpawnTimer = 0f // 상한 음식 떨어지는 시간을 기록할 변수
    private val debuffpawnInterval = 7f // 상한 음식 떨어지는 간격
    private var slowDebuffTimer = 0f  // 10초 느려짐 재는 변수

    private var itemSpawnTimer = 0f // 아이템 떨어지는 시간을 기록할 변수
    private val itemSpawnInterval = 10f // 아이템 떨어지는 간격 => 5~7초 랜덤으로 떨어지게끔: 코드 변경해보기
    private var fastBuffTimer = 0f          // 속도 1.2배 - 7초 재는 시계
    private var scoreMultiplierTimer = 0f   // 점수 1.5배 - 7초 재는 시계


    private var score = 0 // 점수 변수
    private var gameTime= 180f // 제한시간 변수


    //모든 음식들을 여러개 담을 리스트
    private val enemies = mutableListOf<ExampleEnemy>()
    //음식의 이미지와 점수를 한 묶음으로 담을 데이터 통
    private data class FoodData(val texture: Texture, val score: Int)
    private val enemyTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st1-food-rice.png")),10),
        FoodData(Texture(Gdx.files.internal("st1-food-kimchi.png")),20),
        FoodData(Texture(Gdx.files.internal("st1-food-mandu.png")),30),
        FoodData(Texture(Gdx.files.internal("st1-food-ramen.png")),40),
        FoodData(Texture(Gdx.files.internal("st1-food-cutlet.png")),50)
    )
    private val rareTexture = listOf(
        FoodData(Texture(Gdx.files.internal("st1-rare-5chup.png")),100),
        FoodData(Texture(Gdx.files.internal("st1-rare-galbi.png")),500)
    )
    private val debuffTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st1-de-rice.png")),-100),
        FoodData(Texture(Gdx.files.internal("st1-de-mandu.png")),0)
    )
    private val itemTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st1-item-beer.png")),-3),
        FoodData(Texture(Gdx.files.internal("st1-item-cola.png")),-2)
    )



    private fun spawnEnemy() {
        val randomFood = enemyTextures.random()
        //val randomTexture = enemyTextures.random() //enemyTextures 중에서 랜덤으로
        //val itemScore = if (randomTexture.toString().contains("rice")) 10 else 50
        val enemy = ExampleEnemy(
            x = Random.nextFloat() * (worldWidth - 40f),
            //Random.nextFloat(week6 p.13참고), 왼쪽 벽 ~ 오른쪽 벽까지 랜덤으로 생성
            // enemy 크기 만큼 빼주어 오른쪽 벽에 붙어서 떨어질때 물체가 다 보이도록 설정
            y = worldHeight + 100f,// 화면 경계 밖에서 생성되어 떨어지도록
            minY = -100f,               // 화면 넘어로 물체가 사라지도록 설정
            maxY = worldHeight + 100f, // 화면 넘어로 물체가 사라지도록 설정
            texture = randomFood.texture, // 뽑힌 음식
            scoreValue = randomFood.score // 뽑힌 음식의 점수
        )
        enemies.add (enemy)
        add(enemy)
    }
    private fun spawnRareFood() {
        val randomRare = rareTexture.random() // 희귀 리스트 뽑기
        val enemy = ExampleEnemy(
            x = Random.nextFloat() * (worldWidth - 40f),
            y = worldHeight + 100f,
            minY = -100f,
            maxY = worldHeight + 100f,
            texture = randomRare.texture,
            scoreValue = randomRare.score
        )
        enemies.add(enemy)
        add(enemy)
    }
    private fun spawnDebuffFood() {
        val randomDebuff = debuffTextures.random()
        val enemy = ExampleEnemy(
            x = Random.nextFloat() * (worldWidth - 40f),
            y = worldHeight + 100f,
            minY = -100f,
            maxY = worldHeight + 100f,
            texture = randomDebuff.texture,
            scoreValue = randomDebuff.score
        )
        enemies.add(enemy)
        add(enemy)
    }
    private fun spawnItemFood() {
        val randomItem = itemTextures.random()
        val enemy = ExampleEnemy(
            x = Random.nextFloat() * (worldWidth - 40f),
            y = worldHeight + 100f,
            minY = -100f,
            maxY = worldHeight + 100f,
            texture = randomItem.texture,
            scoreValue = randomItem.score
        )
        enemies.add(enemy)
        add(enemy)
    }



    // 적 — 월드 상단에서 아래로 이동
    //private val enemy = ExampleEnemy(
    //    x = Random.nextFloat() * (worldWidth - 40f),
        //Random.nextFloat(week6 p.13참고), 왼쪽 벽 ~ 오른쪽 벽까지 랜덤으로 생성
        // enemy 크기 만큼 빼주어 오른쪽 벽에 붙어서 떨어질때 물체가 다 보이도록 설정
    //    y = worldHeight + 100f, // 화면 경계 밖에서 생성되어 떨어지도록
    //    minY = -100f,               // 화면 넘어로 물체가 사라지도록 설정
    //    maxY = worldHeight + 100f,  // 화면 넘어로 물체가 사라지도록 설정
    //)

    // 현재 게임 상태 — 입력/충돌에 따라 IN_PLAY ↔ GAME_OVER 로 전환된다.
    private var state = GameState.IN_PLAY

    // ── 체스판 배경 설정 (drawBackground() 에서 사용) ──
    //   이게 없으면 검은 배경뿐이라 카메라(WASD) 이동이 눈에 안 보인다.
    //   학생은 자기 게임에선 다른 배경을 그리거나, 그냥 두면 검은 배경이다.
    //
    //   tile.png 는 흰색 64x64 정사각형 한 장. 같은 텍스처에 batch.color 를
    //   바꿔가며 두 가지 색으로 그리는 트릭(틴트) 으로 체스판을 만든다.
    private val tileTexture = Texture(Gdx.files.internal("st1_bg.png"))
//    private val bgColorDark = Color(1f, 1f, 1f, 1f)
//    private val bgColorLight = Color(1f, 1f, 1f, 1f)
//    private val tileSize = 1000f

    /**
     * 생성자 본문 — 월드에 플레이어와 적을 등록한다.
     *   이렇게 등록해야 update / draw 루프에 포함된다.
     */
    init {
        add(player)
        //add(enemy)
    }

    /**
     * 매 프레임 게임 로직 — 모든 '입력 처리·상태 변경' 은 이 안에서.
     *
     * 상태별로 해야 할 일이 완전히 다르므로 when 으로 분기한다.
     * (입력 처리가 render() 가 아닌 update() 에 있는 이유:
     *  '로직과 그리기의 분리' — render 는 매 프레임 그리는 일에만 집중하고,
     *  상태 변화·입력은 update 가 책임진다.)
     */
    override fun update(delta: Float) {
        when (state) {
            GameState.IN_PLAY -> updateInPlay(delta)
            GameState.GAME_OVER -> updateGameOver()
        }
    }

    /** IN_PLAY 상태에서 매 프레임 처리 — 카메라 이동, 객체 갱신, 충돌 체크. */
    private fun updateInPlay(delta: Float) {
        // ── 카메라 이동 (WASD) ──
        //   offsetX/Y 를 바꾸면 카메라가 월드 안에서 움직인다.
        val cameraSpeed = 200f * delta
        if (InputHandler.isKeyPressed(InputHandler.W)) offsetY += cameraSpeed
        if (InputHandler.isKeyPressed(InputHandler.S)) offsetY -= cameraSpeed
        if (InputHandler.isKeyPressed(InputHandler.A)) offsetX -= cameraSpeed
        if (InputHandler.isKeyPressed(InputHandler.D)) offsetX += cameraSpeed

        // 카메라가 월드 경계 밖을 보여주지 않도록 clamp.
        //   보여주는 영역이 [offset, offset+screen] 이어야 하므로
        //   offset 은 0 ~ (world - screen) 범위여야 한다.
        offsetX = offsetX.coerceIn(0f, worldWidth - screenWidth)
        offsetY = offsetY.coerceIn(0f, worldHeight - screenHeight)

        // 시간 타이머
        gameTime -= delta
        if (gameTime <= 0f) {
            gameTime = 0f // 시간이 마이너스로 가지 않게 0으로 고정
            state = GameState.GAME_OVER // 0초가 되면 게임 오버!
        }

        spawnTimer += delta  // 매 프레임마다 지난 시간을 더함
        if (spawnTimer >= spawnInterval) {
            spawnEnemy()     // 1초가 넘으면 음식 생성
            spawnTimer = 0f
        // 다음 음식을 위해 타이머를 다시 0으로 초기화 => 초기화 하지 않으면 타이머는 1초,2초,3초...커지기만 함
        //=> 1초 이후부터는 게임이 끝날 때까지 초당 60번마다 음식을 미친 듯이 쏟아냄 -> Fever 타임에 적용하면 좋을듯
        }
        rareSpawnTimer += delta //  희귀 음식 타이머 작동
        if (rareSpawnTimer >= rareSpawnInterval) {
            spawnRareFood()   // 일정 시간 후 희귀 음식 등장
            rareSpawnTimer = 0f
        }

        // 느려짐 복구 - 10초가 지나면 원래 속도로
        if (slowDebuffTimer > 0f) {
            slowDebuffTimer -= delta
            if (slowDebuffTimer <= 0f) {
                player.speed = 200f // 다시 원래 속도로 달리기
                player.setTexture(playerTextures[0].texture)
            }
        }
        // 상한 음식 소환
        debuffSpawnTimer += delta
        if (debuffSpawnTimer >= debuffpawnInterval) {
            spawnDebuffFood()
            debuffSpawnTimer = 0f
        }

        //아이템 - 콜라 시계 (7초 뒤에 원래 속도로)
        if (fastBuffTimer > 0f) {
            fastBuffTimer -= delta
            if (fastBuffTimer <= 0f) {
                player.speed = 200f // 원상복구
                player.setTexture(playerTextures[0].texture)
            }
        }

        // 아이템 - 맥주 시계 (7초 뒤에 점수 1.5배 끝)
        if (scoreMultiplierTimer > 0f) {
            scoreMultiplierTimer -= delta
            if (scoreMultiplierTimer <= 0f) {
                player.setTexture(playerTextures[0].texture) // 맥주 타이머 끝나면 이미지 원상복구!
            }
        }

        //아이템 소환
        itemSpawnTimer += delta
        if (itemSpawnTimer >= itemSpawnInterval) {
            spawnItemFood()
            itemSpawnTimer = 0f
        }

        // ── 1) 게임 객체 갱신 — 각자 한 프레임씩 진행 ──
        updateAllObjects(delta)

        // ── 2) 상호작용 결정 — 누가 누구와 부딪혀 어떻게 되는지 ──
        //   collidesWith 는 GameObject 의 메서드 → 모든 게임 객체가 자동으로 가짐.
        //   이 예제에선 충돌 시 객체를 죽이지 않고 게임 상태만 바꾼다.
        //   (총알 게임이라면 여기서 bullet.kill(), enemy.kill() 같은 처리)
//        if (player.collidesWith(enemy)) {
//            state = GameState.GAME_OVER
//        }
        // 상호작용 결정 — 누가 누구와 부딪혀 어떻게 되는지
//        for (enemy in enemies) {
//            if (player.collidesWith(enemy)) {
//                state = GameState.GAME_OVER
//                break
//            }
//        }


        //부딪힌 음식들을 임시로 담아둘 빈 리스트
        val enemiesToRemove = mutableListOf<ExampleEnemy>()

        for (enemy in enemies) {
            if (player.collidesWith(enemy)) {

                if (enemy.scoreValue == 0) { // 상한 만두(0)
                    player.speed = 100f //속도 느려짐
                    slowDebuffTimer = 10f // 10초 타이머 시작
                    player.setTexture(playerTextures[1].texture) // 디버프 이미지로 바꾸기
                    enemiesToRemove.add(enemy) // 만두 먹은 후 부딪힌 음식들 임시로 담아둘 빈 리스트에 넣기
                    continue
                // 만두 처리 끝났으니 아래 점수 계산은 쳐다보지도 말고 다음 음식으로 넘어가기
                // continue가 없으면 곧바로 그 밑에 있는 점수 계산 코드인 score += (enemy.scoreValue * 1.5f).toInt()까지 실행
                // -> continue 안 할 경우 점수 오작동 발생!
                }
                else if (enemy.scoreValue == -2) { //콜라(-2)
                    player.speed = 240f // 기본 속도 200의 1.2배인 240으로 빨라짐
                    fastBuffTimer = 7f // 7초 타이머
                    player.setTexture(playerTextures[2].texture)
                    enemiesToRemove.add(enemy)
                    continue
                }
                else if (enemy.scoreValue == -3) { //맥주(-3)
                    scoreMultiplierTimer = 7f // 7초 타이머
                    player.setTexture(playerTextures[2].texture)
                    enemiesToRemove.add(enemy)
                    continue
                }
                else if (enemy.scoreValue == -100) { //상한 밥(+100)
                    slowDebuffTimer = 10f // 10초 타이머
                    player.setTexture(playerTextures[1].texture)
                }// 점수 계산은 아래에서 정상적으로 처리해야 하므로  continue X

                // 맥주 효과 작동 중일 때: 모든 음식을 1.5배로 뻥튀기 -> 정수로 바꿔서 합함
                if (scoreMultiplierTimer > 0f) {
                    score += (enemy.scoreValue * 1.5f).toInt()
                } else {
                    score += enemy.scoreValue // 맥주 효과가 없을 때: 원래 점수 그대로 획득/차감
                }

                enemiesToRemove.add(enemy)
            }
        }

        for (enemy in enemiesToRemove) {
            remove(enemy) //화면(월드)에서 안 보이게 지움
            enemies.remove(enemy) //음식 리스트 명단에서도 지움
        }
//
        // ── 3) 죽은 객체 정리 ──
        //   현재 예제에선 아무 것도 안 죽으므로 영향 없지만,
        //   bullet/enemy 가 추가될 때를 대비한 표준 흐름이다.
        removeDead()
    }

    /** GAME_OVER 상태에서 매 프레임 처리 — ESC 입력만 감시한다. */
    private fun updateGameOver() {
        // ESC 키가 '막 눌린 순간' 앱 종료.
        //   isKeyJustPressed 로 한 이유: 누르고 있는 동안 매 프레임 exit 호출되지 않게.
        if (InputHandler.isKeyJustPressed(InputHandler.ESCAPE)) {
            Gdx.app.exit()
        }
    }

    /**
     * 배경 그리기 — GameWorld.drawBackground(batch) 를 override.
     *
     * 부모가 이미 batch.begin() 을 호출한 상태에서 이 함수를 부르므로,
     * 여기선 batch.draw() 호출만 하면 된다. (begin/end 를 또 부르면 안 된다)
     *
     * 카메라(offset) 에 따라 타일 위치가 바뀌어 이동감을 준다.
     *   타일 인덱스 자체는 월드 좌표 격자에서 변하지 않지만,
     *   각 타일을 그릴 때 offset 만큼 빼서 화면 좌표로 변환한다.
     *
     * 색을 입히는 방법:
     *   batch.color 를 바꾼 뒤 batch.draw 하면 텍스처가 그 색으로 곱해져 그려진다.
     *   tile.png 가 흰색이라 어떤 색이든 그대로 적용된다.
     *   끝에 다시 흰색으로 되돌려두지 않으면 그 다음 그리는 것까지 영향을 받으니 주의.
     */
    override fun drawBackground(batch: SpriteBatch) {
//        // 현재 카메라 시작점이 속한 타일 인덱스 (여유분으로 -1)
//        val startCol = floor(offsetX / tileSize).toInt() - 1
//        val startRow = floor(offsetY / tileSize).toInt() - 1
//        // 화면을 채우는 데 필요한 타일 개수 (여유분 +3)
//        val cols = (screenWidth / tileSize).toInt() + 3
//        val rows = (screenHeight / tileSize).toInt() + 3
//
//        for (row in startRow until startRow + rows) {
//            for (col in startCol until startCol + cols) {
//                // 행+열이 짝수면 어둡게, 홀수면 밝게 → 체스판 패턴
//                batch.color = if ((row + col) % 2 == 0) bgColorDark else bgColorLight
//
//                // 월드 좌표의 타일 위치에서 offset 만큼 빼면 화면 좌표
//                val drawX = col * tileSize - offsetX
//                val drawY = row * tileSize - offsetY
//                batch.draw(tileTexture, drawX, drawY, tileSize, tileSize)
//            }
//        }

            // 1. 이미지 원래 색상 그대로 나오게 흰색(기본값) 설정r
            batch.color = Color.WHITE

            // 2. 화면 (0,0) 위치부터 화면 전체 너비/높이만큼 그리기
            batch.draw(tileTexture, 0f, 0f, screenWidth, screenHeight)

    }

    /**
     * 매 프레임 그리기 — 부모가 배경·객체까지 그려준 뒤, 텍스트 UI 를 얹는다.
     *
     * 이 함수에서는 '그리기' 만 한다. 입력 처리·상태 변경은 update() 의 책임.
     *
     * 주의: super.render(delta) 가 화면 clear + 배경 + 객체까지 그리므로,
     *       텍스트는 반드시 super 호출 **이후** 그려야 가려지지 않는다.
     */
    override fun render(delta: Float) {
        super.render(delta)

        // ── 항상 보이는 UI ──
        drawHud()

        // ── 상태별로 그리는 것이 다름 ──
        when (state) {
            GameState.IN_PLAY -> drawGameStartOverlay(delta)
                // 플레이 중에는 추가로 그릴 것 없음

            GameState.GAME_OVER -> drawGameOverOverlay()
        }
    }

    /** 항상 화면에 표시되는 정보 — HP 표시와 월드 중앙 표지. */
    private fun drawHud() {
        // 1) UI 텍스트 (화면 고정) — 좌측 상단 HP 표시.
        //    카메라가 움직여도 항상 이 위치에 있다.
        drawTextOnScreen(
            text = "Stage 1",
            x = 10f,
            y = screenHeight - 10f,   // 화면 y 축은 위로 증가 → 맨 위가 screenHeight
            color = Color.BLACK,
            scale = 2f
        )
        // 화면 오른쪽 위에 점수 그리기 추가
        drawTextOnScreen(
            text = "Score: $score",
            x = screenWidth - 150f, // 화면 오른쪽 끝에서 살짝 왼쪽으로
            y = screenHeight - 10f, // 화면 맨 위
            color = Color.WHITE,
            scale = 1.5f
        )
        // 화면 오른쪽 위에 타이머 그리기 추가
        val minutes = (gameTime / 60).toInt() // 60으로 나누면 분
        val seconds = (gameTime % 60).toInt() // 60으로 나눈 나머지는 초
        var timeString = ""
        if(seconds < 10) timeString = "Time: $minutes:0$seconds" else timeString = "Time: $minutes: $seconds"
        drawTextOnScreen(
            text = timeString,
            x = screenWidth - 150f, // 점수와 같은 가로줄(x)
            y = screenHeight - 40f, // 점수 글씨보다 조금 더 아래쪽(y)에 배치
            color = Color.GREEN,    // 눈에 잘 띄게 초록색으로 설정
            scale = 1.5f
        )

        // 2) 월드 텍스트 (월드 좌표) — 월드 정중앙에 "WORLD CENTER".
        //    WASD 로 카메라를 움직이면 이 글자도 화면에서 움직인다.
        //drawTextInWorld(
            //text = "WORLD CENTER",
            //worldX = worldWidth / 2 - 70f,
            //worldY = worldHeight / 2,
            //color = Color.CYAN,
            //scale = 1.5f
        //)
    }
    private var startTimer = 1f
    //게임 시작 시 화면에 띄우는 안내 메세지 => 3초 후 사라지게 만들어야함...
    private fun drawGameStartOverlay(delta: Float) {
        if(startTimer > 0) {
            startTimer -= delta
            drawTextOnScreen(
                text = "Game start!",
                x = screenWidth / 2 - 70f,
                y = screenHeight / 2,
                color = Color.BLACK,
                scale = 2f
            )
        }else if(startTimer <= 0) {
            drawTextOnScreen(
                text = " ",
                x = screenWidth / 2 - 70f,
                y = screenHeight / 2
            )
        }

    }
    /** 게임 오버 시 화면 중앙에 띄우는 안내 메시지. */
    private fun drawGameOverOverlay() {
        drawTextOnScreen(
            text = "Game Over!",
            x = screenWidth / 2 - 70f,
            y = screenHeight / 2,
            color = Color.RED,
            scale = 2f
        )
        drawTextOnScreen(
            text = "Press ESC to exit",
            x = screenWidth / 2 - 50f,
            y = screenHeight / 2 - 40f,
            color = Color.WHITE,
            scale = 1f
        )
    }

    /** 화면이 닫힐 때 — 부모도 dispose 한 뒤 우리만의 자원도 해제. */
    override fun dispose() {
        super.dispose()
        tileTexture.dispose()
    }
}

