package com.oop.game.example

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.oop.game.GameWorld
import com.oop.game.InputHandler
import kotlin.math.floor
import kotlin.random.Random
import com.badlogic.gdx.graphics.GL20


/**
 * 하늘에서 음식이 내린다면,,,
 * 하늘에서 떨어지는 음식을 마구 먹고 싶다. 하지만 이를 방해하는 녀석들과 먹으면 안되는 썩은 음식들 세상은 호락호락하지 않다.
 * 이를 피해 맛있는 음식만 먹어서 점수를 올려보자,,, 나는 얼마나 먹을 수 있지?
 *
 *  ── 조작법 ──
 *   ▸ 화살표 키  : 플레이어 이동
 *   ▸ R        : 재 시작
 *   ▸ ESC       : 게임 오버 후 종료
 *
 *  ── 게임 상태 ──
 *   IN_PLAY   : 일반 진행 (이동·충돌 체크)
 *   GAME_OVER : 충돌 후 정지, ESC 입력 대기
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
    private val st2playerTextures = listOf(
        ChrData(Texture(Gdx.files.internal("char2-default.png"))),
        ChrData(Texture(Gdx.files.internal("char2-debuff.png"))),
        ChrData(Texture(Gdx.files.internal("char2-buff.png")))
    )
    private val st3playerTextures = listOf(
        ChrData(Texture(Gdx.files.internal("char3-default.png"))),
        ChrData(Texture(Gdx.files.internal("char3-debuff.png"))),
        ChrData(Texture(Gdx.files.internal("char3-buff.png")))
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

    //private val debuffSpawnInterval = 7f // 상한 음식 떨어지는 간격

    private var slowDebuffTimer = 0f  // 디버프 지속시간 변수
    private var dustDebuffTimer = 0f // 먼지 디버프 지속시간

    private var itemSpawnTimer = 0f // 아이템 떨어지는 시간을 기록할 변수
    private var itemSpawnInterval = Random.nextDouble(5.0, 7.0).toFloat()
    // 아이템이 5초 이상 10초 미만으로 랜덤하게 떨어짐.
    //private val itemSpawnInterval = 10f // 아이템 떨어지는 간격 => 5~7초 랜덤으로 떨어지게끔: 코드 변경해보기

    private var fastBuffTimer = 0f          // 속도 1.2배 - 7초 재는 시계
    private var scoreMultiplierTimer = 0f   // 점수 1.5배 - 7초 재는 시계
    private var tomatoTimer = 0f
    private var breadTimer = 0f
    private var feverTimer = 0f              // 피버 타임 남은 시간을 재는 시계
    private var feverSpawnTimer = 0f         // 피버 타임 전용 쏟아지는 타이머

    //    private val feverDuration = 7f           // 핫식스 지속 시간 (7초)

    private val feverSpawnInterval = 0.3f    // 피버 타임- 0.3초마다 미친 듯이 쏟아짐

    private var score = 0 // 점수 변수
    private var gameTime= 180f // 제한시간 변수

    // GameOver 이후 랭킹 구현 로컬 데이터 저장소
    private val prefs = Gdx.app.getPreferences("MyGamePreference")
    private var rankingText = ""

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
        FoodData(Texture(Gdx.files.internal("st1-item-beer.png")),1),
        FoodData(Texture(Gdx.files.internal("st1-item-cola.png")),2)
    )
    private val St2enemyTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st2-fries.png")),20),
        FoodData(Texture(Gdx.files.internal("st2-cheese.png")),30),
        FoodData(Texture(Gdx.files.internal("st2-burger.png")),40),
        FoodData(Texture(Gdx.files.internal("st2-chicken.png")),50),
        FoodData(Texture(Gdx.files.internal("st2-pizza.png")),60)
    )
    private val St2rareTexture = listOf(
        FoodData(Texture(Gdx.files.internal("st2-yeobdduck.png")),100),
        FoodData(Texture(Gdx.files.internal("st2-goldchicken.png")),500)
    )
    private val St2debuffTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st2-de-brcl.png")),3),
        FoodData(Texture(Gdx.files.internal("st2-de-carrot.png")),4),
        FoodData(Texture(Gdx.files.internal("st2-de-eggplant.png")),-300),
        FoodData(Texture(Gdx.files.internal("st2-de-tomato.png")),5)
    )
    private val St2itemTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st2-item-cola.png")),6),
        FoodData(Texture(Gdx.files.internal("st2-item-beer.png")),7),
        FoodData(Texture(Gdx.files.internal("st2-item-hot6.png")),8)
    )
    private val St3enemyTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st3-salad.png")),30),
        FoodData(Texture(Gdx.files.internal("st3-soup.png")),40),
        FoodData(Texture(Gdx.files.internal("st3-pasta.png")),50),
        FoodData(Texture(Gdx.files.internal("st3-robster.png")),60),
        FoodData(Texture(Gdx.files.internal("st3-steak.png")),70)
    )
    private val St3rareTexture = listOf(
        FoodData(Texture(Gdx.files.internal("st3-icecream.png")),200),
        FoodData(Texture(Gdx.files.internal("st3-cake.png")),500)
    )
    private val St3debuffTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st3-de-shoes.png")),9),
        FoodData(Texture(Gdx.files.internal("st3-de-dish.png")),11),
        FoodData(Texture(Gdx.files.internal("st3-de-rat.png")),-500),
        FoodData(Texture(Gdx.files.internal("st3-de-bread.png")),12),
        FoodData(Texture(Gdx.files.internal("st3-de-dust.png")),13)
    )
    private val St3itemTextures = listOf(
        FoodData(Texture(Gdx.files.internal("st3-item-cola.png")),21),
        FoodData(Texture(Gdx.files.internal("st3-item-beer.png")),22),
        FoodData(Texture(Gdx.files.internal("st3-item-hot6.png")),8),
        FoodData(Texture(Gdx.files.internal("st3-item-wine.png")),24)
    )
    private val St3obstacleTextures = listOf(
        FoodData(Texture(Gdx.files.internal("item-weighing.png")),26),
        FoodData(Texture(Gdx.files.internal("pellican-01.png")),27),
        FoodData(Texture(Gdx.files.internal("pellican-02.png")),27)
    )
    // 현재 화면과 리스트에 남아있는 모든 음식을 깔끔하게 지우는 함수
    private fun clearEnemies() {
        // 월드(화면)에서 삭제
        for (enemy in enemies) {
            remove(enemy)
        }
        // 음식 리스트 비우기
        enemies.clear()
    }

    private fun createEnemy(texture: Texture, scoreValue: Int): ExampleEnemy {
        return ExampleEnemy(
            x = Random.nextFloat() * (worldWidth - 40f),
            y = worldHeight + 100f,
            minY = -100f,
            maxY = worldHeight + 100f,
            texture = texture,
            scoreValue = scoreValue
        )
    }

    private fun spawnEnemy() {
        // 현재 스테이지에 따라 참조할 데이터 리스트를 다르게 지정
        val currentFoodList = when (currentStage) {
            1 -> enemyTextures
            2 -> St2enemyTextures
            else -> St3enemyTextures
        }

        // 선택된 리스트에서 랜덤으로 음식을 뽑기
        val randomFood = currentFoodList.random()
//        val randomFood = enemyTextures.random()

        //val randomTexture = enemyTextures.random() //enemyTextures 중에서 랜덤으로
        //val itemScore = if (randomTexture.toString().contains("rice")) 10 else 50
//        val enemy = ExampleEnemy(
//            x = Random.nextFloat() * (worldWidth - 40f),
//            //Random.nextFloat(week6 p.13참고), 왼쪽 벽 ~ 오른쪽 벽까지 랜덤으로 생성
//            // enemy 크기 만큼 빼주어 오른쪽 벽에 붙어서 떨어질때 물체가 다 보이도록 설정
//            y = worldHeight + 100f,// 화면 경계 밖에서 생성되어 떨어지도록
//            minY = -100f,               // 화면 넘어로 물체가 사라지도록 설정
//            maxY = worldHeight + 100f, // 화면 넘어로 물체가 사라지도록 설정
//            texture = randomFood.texture, // 뽑힌 음식
//            scoreValue = randomFood.score // 뽑힌 음식의 점수
//        )
        val enemy = createEnemy(randomFood.texture, randomFood.score)
        enemies.add (enemy)
        add(enemy)
    }
    private fun spawnRareFood() {
        val currentFoodList = when (currentStage) {
            1 -> rareTexture
            2 -> St2rareTexture
            else -> St3rareTexture // 스테이지3로 이용
        }
        val randomRare = currentFoodList.random() // 희귀 리스트 뽑기
//            val enemy = ExampleEnemy(
//                x = Random.nextFloat() * (worldWidth - 40f),
//                y = worldHeight + 100f,
//                minY = -100f,
//                maxY = worldHeight + 100f,
//                texture = randomRare.texture,
//                scoreValue = randomRare.score
//            )
        val enemy = createEnemy(randomRare.texture, randomRare.score)
        enemies.add(enemy)
        add(enemy)
    }
    private fun spawnDebuffFood() {
        val currentFoodList = when (currentStage) {
            1 -> debuffTextures
            2 -> St2debuffTextures
            else -> St3debuffTextures// 스테이지3로 이용
        }
        val randomDebuff = currentFoodList.random()
//        val enemy = ExampleEnemy(
//            x = Random.nextFloat() * (worldWidth - 40f),
//            y = worldHeight + 100f,
//            minY = -100f,
//            maxY = worldHeight + 100f,
//            texture = randomDebuff.texture,
//            scoreValue = randomDebuff.score
//       )
        val enemy = createEnemy(randomDebuff.texture, randomDebuff.score)
        enemies.add(enemy)
        add(enemy)
    }
    private fun spawnItemFood() {
        val currentFoodList = when (currentStage) {
            1 -> itemTextures
            2 -> St2itemTextures
            else ->St3itemTextures // 스테이지3로 이용
        }
        val randomItem = currentFoodList.random()
/*        val enemy = ExampleEnemy(
            x = Random.nextFloat() * (worldWidth - 40f),
            y = worldHeight + 100f,
            minY = -100f,
            maxY = worldHeight + 100f,
            texture = randomItem.texture,
            scoreValue = randomItem.score
        )*/
        val enemy = createEnemy(randomItem.texture, randomItem.score)
        enemies.add(enemy)
        add(enemy)
    }
    // 현재 게임 상태 — 입력/충돌에 따라 IN_PLAY ↔ GAME_OVER 로 전환된다.
    private var state = GameState.IN_PLAY
    private val St1Texture = Texture(Gdx.files.internal("st1_bg.png"))
    private val St2Texture = Texture(Gdx.files.internal("st2_bg.png"))
    private val St3Texture = Texture(Gdx.files.internal("st3_bg.png"))
    private val gameOverTexture = Texture(Gdx.files.internal("gameover_bg.png"))

    private var currentStage = 1 // 현재 스테이지 번호
   // private var stageTimer = 0f // 스테이지 타이머

//    private val bgColorDark = Color(1f, 1f, 1f, 1f)
//    private val bgColorLight = Color(1f, 1f, 1f, 1f)
//    private val tileSize = 1000f

    init {
        add(player)
    }

    override fun update(delta: Float) {
        when (state) {
            GameState.IN_PLAY -> updateInPlay(delta)
            GameState.GAME_OVER -> updateGameOver()
        }
    }

    /** IN_PLAY 상태에서 매 프레임 처리 — 카메라 이동, 객체 갱신, 충돌 체크. */
    private fun updateInPlay(delta: Float) {
        // 시간 타이머
        gameTime -= delta
        if (gameTime <= 0f) {
            gameTime = 0f // 시간이 마이너스로 가지 않게 0으로 고정
            state = GameState.GAME_OVER // 0초가 되면 게임 오버!

            // 랭킹 저장되는 리스트(중복 제외)
            val savedRanking = prefs.getString("ranking_list", "")

            val scoreList = mutableListOf<Int>()
            if(savedRanking.isNotEmpty()) {
                scoreList.addAll(savedRanking.split(",").map { it.toInt() })
            }
            if (!scoreList.contains(score)){
                scoreList.add(score)
            }

            val topScores = scoreList.sortedDescending().take(10)

            prefs.putString("ranking_list", topScores.joinToString(","))
            prefs.flush()

            rankingText = topScores.mapIndexed {index, s ->
                "${index + 1}St: ${s}"}.joinToString("\n")

            return
        }

        // 음식 & 피버 타임
        if (feverTimer > 0f) {
            // 피버 타임
            feverTimer -= delta
            feverSpawnTimer += delta

            if (feverSpawnTimer >= feverSpawnInterval) {
                spawnRareFood()
                feverSpawnTimer = 0f
            }

        } else {
            // 일반 타임
            // 1. 일반 음식
            spawnTimer += delta
            if (spawnTimer >= spawnInterval) {
                if (tomatoTimer <= 0f) {
                    spawnEnemy()
                }
                spawnTimer = 0f
            }

            // 2. 희귀 음식
            rareSpawnTimer += delta
            if (rareSpawnTimer >= rareSpawnInterval) {
                if (tomatoTimer <= 0f) {
                    spawnRareFood()
                }
                rareSpawnTimer = 0f
            }

            // 3. 디버프 음식
            val debuffSpawnInterval = if (tomatoTimer > 0f) {
                tomatoTimer -= delta // 토마토 5초 지속시간 차감
                Random.nextDouble(0.6, 1.0).toFloat() // 토마토 발동 중에는 0.6~1초 랜덤으로 디버프 음식 떨어짐
            }else if(breadTimer > 0) {
                breadTimer -= delta
                Random.nextDouble(0.6, 1.0).toFloat()
            }else {
                if (currentStage == 1) 7f else if(currentStage == 2) 5f else 4f  // 스테이지1: 7초, 스테이지2: 5초, 스테이지3: 4초 => 기존 디버프 떨어지는 간격
            }

            debuffSpawnTimer += delta
            if (debuffSpawnTimer >= debuffSpawnInterval) {
                spawnDebuffFood()
                debuffSpawnTimer = 0f
            }

            // 4. 아이템 스폰
            itemSpawnTimer += delta
            if (itemSpawnTimer >= itemSpawnInterval) {
                if (tomatoTimer <= 0f) {
                    spawnItemFood()
                }
                itemSpawnTimer = 0f
                itemSpawnInterval = Random.nextDouble(5.0, 7.0).toFloat()
            }
        }

//        spawnTimer += delta  // 매 프레임마다 지난 시간을 더함
//        if (spawnTimer >= spawnInterval) {
//            //토마토 효과가 아닐 때만 일반 음식 1초 후 생성
//            if (tomatoTimer <= 0f) {
//                spawnEnemy()
//            }
//            //spawnEnemy()     // 1초가 넘으면 음식 생성
//            spawnTimer = 0f
//        // 다음 음식을 위해 타이머를 다시 0으로 초기화 => 초기화 하지 않으면 타이머는 1초,2초,3초...커지기만 함
//        //=> 1초 이후부터는 게임이 끝날 때까지 초당 60번마다 음식을 미친 듯이 쏟아냄 -> Fever 타임에 적용하면 좋을듯
//        }

        // 스테이지 시간 누적 및 스테이지 2 전환
        if (currentStage == 1 && gameTime <= 140f) {
            currentStage = 2
            // 참고) 여기서 spawnInterval = 0.5f 처럼 생성 속도를 빠르게 조절 가능
            clearEnemies() // 지우기
            player.setTexture(st2playerTextures[0].texture)
        }
        // 스테이지 3 전환
        else if (currentStage == 2 && gameTime <= 80f) {
            currentStage = 3
            clearEnemies()
            player.setTexture(st3playerTextures[0].texture)
        }

        rareSpawnTimer += delta //  희귀 음식 타이머 작동
        if (rareSpawnTimer >= rareSpawnInterval) {
            // 토마토 효과가 아닐 때만 희귀 음식 일정 시간 후 등장
            if (tomatoTimer <= 0f) {
                spawnRareFood()
            }
            //spawnRareFood()   // 일정 시간 후 희귀 음식 등장
            rareSpawnTimer = 0f
        }

        // 느려짐 복구 - 10초가 지나면 원래 속도로
        if (slowDebuffTimer > 0f) {
            slowDebuffTimer -= delta
            if (slowDebuffTimer <= 0f) {
                player.speed = 200f // 다시 원래 속도로 달리기
                when (currentStage) {
                    1 -> player.setTexture(playerTextures[0].texture)
                    2 -> player.setTexture(st2playerTextures[0].texture)
                    3 -> player.setTexture(st3playerTextures[0].texture)
                }
            }
        }

        //먼지 디버프 실시간 처리 및 타이머 복구
        if (dustDebuffTimer > 0f) {
            dustDebuffTimer -= delta
            // 먼지 디버프 => 모든 음식 속도를 0.5배로 유지
            for (enemy in enemies) {
                enemy.speedMultiplier = 0.5f
            }
            // 시간이 다 끝나면 원래 속도(1.0배)로 복구
            if (dustDebuffTimer <= 0f) {
                for (enemy in enemies) {
                    enemy.speedMultiplier = 1.0f
                }
            }
        } else {
            // 평소(먼지 효과가 없을 때)에는 모든 음식 속도를 1.0배로 보장
            for (enemy in enemies) {
                enemy.speedMultiplier = 1.0f
            }
        }
//        // 상한 음식 소환
//        val debuffSpawnInterval = if (tomatoTimer > 0f) {
//            tomatoTimer -= delta // 토마토 5초 지속시간 차감
//            Random.nextDouble(0.6, 1.0).toFloat() // 토마토 발동 중에는 0.6~1초 랜덤
//        } else {
//            if (currentStage == 1) 7f else 5f // 스테이지1: 7초, 스테이지2: 5초
//        }
//        debuffSpawnTimer += delta
//        if (debuffSpawnTimer >= debuffSpawnInterval) {
//            spawnDebuffFood()
//            debuffSpawnTimer = 0f // 소환 후 타이머 초기화
//        }

        //아이템 - 콜라 시계 (7초 뒤에 원래 속도로)
        if (fastBuffTimer > 0f) {
            fastBuffTimer -= delta
            if (fastBuffTimer <= 0f) {
                player.speed = 200f // 원상복구
                if (currentStage == 1) {
                    player.setTexture(playerTextures[0].texture)
                } else if(currentStage == 2) {
                    player.setTexture(st2playerTextures[0].texture)
                }else{
                    player.setTexture(st3playerTextures[0].texture)
                }
                //player.setTexture(playerTextures[0].texture)
            }
        }

        // 아이템 - 맥주 시계 (7초 뒤에 점수 즌 끝)
        if (scoreMultiplierTimer > 0f) {
            scoreMultiplierTimer -= delta
            if (scoreMultiplierTimer <= 0f) {
                if (currentStage == 1) {
                    player.setTexture(playerTextures[0].texture)
                } else if(currentStage == 2) {
                    player.setTexture(st2playerTextures[0].texture)
                }else{
                    player.setTexture(st3playerTextures[0].texture)
                }
                //player.setTexture(playerTextures[0].texture) // 맥주 타이머 끝나면 이미지 원상복구!
            }
        }

//        //아이템 소환
//        itemSpawnTimer += delta
//        if (itemSpawnTimer >= itemSpawnInterval) {
//            //토마토 효과가 아닐 때만 아이템 등장
//            if (tomatoTimer <= 0f) {
//                spawnItemFood()
//            }
//            //spawnItemFood()
//            itemSpawnTimer = 0f
//            // 아이템이 떨어질때마다 5~7초 사이로 랜덤하게 떨어지게 하기 위해서 updateInPlay내부에도 설정해줘야함
//            // 설정하지 않을 시에 처음 떨어질때만 랜덤하게 떨어짐!
//            itemSpawnInterval = Random.nextDouble(5.0, 7.0).toFloat()
//        }

        // 게임 객체 갱신 — 각자 한 프레임씩 진행 ──
        updateAllObjects(delta)

        //부딪힌 음식들을 임시로 담아둘 빈 리스트
        val enemiesToRemove = mutableListOf<ExampleEnemy>()

        for (enemy in enemies) {
            if (player.collidesWith(enemy)) {

                when(enemy.scoreValue){
                    0->{ // 상한 만두(0)
                        player.speed = 100f //속도 느려짐
                        slowDebuffTimer = 10f // 10초 타이머 시작
                        player.setTexture(playerTextures[1].texture) // 디버프 이미지로 바꾸기
                        enemiesToRemove.add(enemy) // 만두 먹은 후 부딪힌 음식들 임시로 담아둘 빈 리스트에 넣기
                        continue
                        // 만두 처리 끝났으니 아래 점수 계산은 쳐다보지도 말고 다음 음식으로 넘어가기
                        // continue가 없으면 곧바로 그 밑에 있는 점수 계산 코드인 score += (enemy.scoreValue * 1.5f).toInt()까지 실행
                        // -> continue 안 할 경우 점수 오작동 발생!
                    }
                    1,7,22 -> {// 맥주
                        scoreMultiplierTimer = 7f // 7초 타이머
                        if (currentStage == 1) {
                            player.setTexture(playerTextures[0].texture)
                        } else if(currentStage == 2) {
                            player.setTexture(st2playerTextures[0].texture)
                        }else{
                            player.setTexture(st3playerTextures[0].texture)
                        }
                        //player.setTexture(playerTextures[2].texture)
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    2,6,21 -> { //콜라
                        player.speed = 240f // 기본 속도 200의 1.2배인 240으로 빨라짐
                        fastBuffTimer = 7f // 7초 타이머
                        if (currentStage == 1) {
                            player.setTexture(playerTextures[2].texture) // st1 콜라
                        } else if (currentStage == 2) {
                            player.setTexture(st2playerTextures[2].texture) // st2 콜라
                        }else{
                            player.setTexture(st3playerTextures[2].texture)
                        }
                        //player.setTexture(playerTextures[2].texture)
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    3 -> { // 브로콜리(3)
                        player.speed = 140f // 기본 속도 200의 0.7배인 140 감속
                        fastBuffTimer = 10f // 10초 타이머
                        player.setTexture(st2playerTextures[1].texture)
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    4 -> { // 당근(4)
                        player.setTexture(st2playerTextures[1].texture)
                        gameTime -= 5f // 제한 시간 5초 감소
                        // 만약 시간이 마이너스로 떨어지면 바로 게임오버가 되도록
                        if (gameTime <= 0f) {
                            gameTime = 0f
                            state = GameState.GAME_OVER
                        }
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    5 -> { // 토마토 (5)
                        slowDebuffTimer = 5f
                        tomatoTimer = 5f // 5초 동안 디버프
                        player.setTexture(st2playerTextures[1].texture)
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    8 -> { // 핫식스(8)
                        feverTimer = 7f // 7초
                        feverSpawnTimer = 0f
                        if (currentStage == 2) {
                            player.setTexture(st2playerTextures[2].texture)
                        } else {
                            player.setTexture(st3playerTextures[2].texture)
                        }
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    -100 -> { // 상한 밥
                        slowDebuffTimer = 3f // 3초 타이머
                        player.setTexture(playerTextures[1].texture)
                        enemiesToRemove.add(enemy)
                    // 점수 계산은 아래에서 정상적으로 처리해야 하므로  continue X
                    }
                    -300 -> { // 가지(-300)
                        slowDebuffTimer = 3f // 3초 타이머
                        player.setTexture(st2playerTextures[1].texture)
                        enemiesToRemove.add(enemy)
                    }
                    9-> { // 더러운 신발(9)
                        player.speed = 140f         // 기본 속도 200의 0.7배인 140으로 감속
                        slowDebuffTimer = 10f       // 10초 동안 디버프
                        player.setTexture(st3playerTextures[1].texture)
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    11 -> { // 더러운 접시(11)
                        player.setTexture(st3playerTextures[1].texture)
                        gameTime -= 5f // 제한 시간 5초 감소
                        // 만약 시간이 마이너스로 떨어지면 바로 게임오버가 되도록
                        if (gameTime <= 0f) {
                            gameTime = 0f
                            state = GameState.GAME_OVER
                        }
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    -500 -> { // 쥐(-500)
                        slowDebuffTimer = 3f
                        player.setTexture(st3playerTextures[1].texture)
                        enemiesToRemove.add(enemy)
                    }
                    12 -> { // 곰팡이 핀 빵 (12)
                        slowDebuffTimer = 10f
                        breadTimer = 5f  // 5초 동안 곰팡이 디버프
                        player.setTexture(st3playerTextures[1].texture)
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    13 -> { // 먼지(13)
                        dustDebuffTimer = 10f // 5초 동안 먼지 효과 지속
                        for (enemy in enemies) {
                            enemy.speedMultiplier = 0.5f
                        }
                        player.setTexture(st3playerTextures[1].texture)
                        enemiesToRemove.add(enemy)
                        continue
                    }
                    24 -> { // 와인(24)
                        gameTime += 15f // 15초 증가
                        player.setTexture(st3playerTextures[2].texture)
                        enemiesToRemove.add(enemy)
                        continue
                    }
                }
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
        removeDead()
    }

    /** GAME_OVER 상태에서 매 프레임 처리 — ESC 입력만 감시한다. */
    private fun updateGameOver() {
        // ESC 키가 '막 눌린 순간' 앱 종료.
        if (InputHandler.isKeyJustPressed(InputHandler.ESCAPE)) {
            Gdx.app.exit()
        }
        // 재시작 R키
        if(InputHandler.isKeyPressed(InputHandler.R)) {
            restartGame()
        }
    }

    override fun drawBackground(batch: SpriteBatch) {
            // 1. 이미지 원래 색상 그대로 나오게 흰색(기본값) 설정r
            batch.color = Color.WHITE

            // 현재 스테이지에 따라 다른 배경 변경
            val currentBg = if (currentStage == 1) St1Texture
            else if(currentStage == 2) St2Texture else St3Texture

            // 화면 (0,0) 위치부터 화면 전체 너비/높이만큼 그리기
            batch.draw(currentBg, 0f, 0f, screenWidth, screenHeight)


    }

    override fun render(delta: Float) {
        super.render(delta)

        // ── 상태별로 그리는 것이 다름 ──
        when (state) {
            GameState.IN_PLAY -> {
                drawHud()
                drawGameStartOverlay(delta)
            }
                // 플레이 중에는 추가로 그릴 것 없음
            GameState.GAME_OVER -> {
                batch.begin()
                batch.color = Color.WHITE
                batch.draw(gameOverTexture, 0f, 0f, screenWidth, screenHeight)
                batch.end()

                drawGameOverOverlay()
            }
        }
    }

    /** 항상 화면에 표시되는 정보 — HP 표시와 월드 중앙 표지. */
    private fun drawHud() {
        // 1) UI 텍스트 (화면 고정) — 좌측 상단 HP 표시.
        //    카메라가 움직여도 항상 이 위치에 있다.
        drawTextOnScreen(
            text = "Stage: $currentStage",
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
        //피버타임 화면에 표시
        if (feverTimer > 0f) {
            drawTextOnScreen(
                text = "FEVER TIME !! (${feverTimer.toInt()}s)",
                x = screenWidth / 2 - 120f,
                y = screenHeight - 50f,
                color = Color.RED,
                scale = 2f
            )
        }

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
        // 좌측 우측 x값 고정
        val leftX = screenWidth / 4f - 80f
        val rightX = screenWidth * 0.7f - 50f

        drawTextOnScreen(
            text = "Game Over!",
            x = leftX - 30f,
            y = screenHeight / 2f + 130f,
            color = Color.RED,
            scale = 2f
        )

        drawTextOnScreen(
            text = "Final Score: $score",
            x = leftX - 20f,
            y = screenHeight / 2f + 60f,
            color = Color.WHITE,
            scale = 1.5f
        )

        drawTextOnScreen(
            text = "Press R To Restart",
            x = leftX - 5f,
            y = screenHeight / 2f - 40f,
            color = Color.GREEN,
            scale = 1.5f
        )

        drawTextOnScreen(
            text = "Press ESC to exit",
            x = leftX - 5f,
            y = screenHeight / 2 - 80f,
            color = Color.WHITE,
            scale = 1f
        )

        // 랭킹 시작 위치
        var bottomY = screenHeight / 2f + 180f

        drawTextOnScreen(
            text = "=== RANK ===",
            x = rightX - 30f,
            y = bottomY,
            color = Color.GREEN,
            scale = 1.2f
        )

        if(rankingText.isNotEmpty()) {
            val rankingLines = rankingText.split("\n")
            for(line in rankingLines) {
                bottomY -= 32f // 랭킹 간격

                drawTextOnScreen(
                    text = line,
                    x = rightX,
                    y = bottomY,
                    color = Color.WHITE,
                    scale = 1.1f
                )
            }
        }
    }

    /** 화면이 닫힐 때 — 부모도 dispose 한 뒤 우리만의 자원도 해제. */
    override fun dispose() {
        super.dispose()
        St1Texture.dispose()
        St2Texture.dispose()
        St3Texture.dispose()
        gameOverTexture.dispose()
    }

    // 게임 재시작 시 처음부터, 게임 배경, 쌓인 시간 0으로 초기화
    private fun restartGame() {
        score = 0
        gameTime = 180f
        startTimer = 1f
        currentStage = 1

        slowDebuffTimer = 0f
        dustDebuffTimer = 0f
        fastBuffTimer = 0f
        scoreMultiplierTimer = 0f
        tomatoTimer = 0f
        breadTimer = 0f
        feverTimer = 0f
        feverSpawnTimer = 0f

        for(enemy in enemies){
            remove(enemy)
        }
        enemies.clear()

        player.x = worldWidth / 2 - 15f
        player.y = 50f
        player.speed = 200f
        player.setTexture(playerTextures[0].texture)

        state = GameState.IN_PLAY
    }
}