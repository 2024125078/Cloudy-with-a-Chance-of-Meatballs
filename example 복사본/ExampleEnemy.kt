package com.oop.game.example

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.oop.game.GameObject

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  적 예제 — enemy.png 이미지, 수평으로 자동 왕복 이동.
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 *  GameObject 를 상속해 만든 '입력 없이 스스로 움직이는' 객체 예제.
 *
 *  핵심 포인트:
 *   ▸ update() 에 입력 처리가 없다 — AI(자율 행동)는 여기서 작성.
 *   ▸ direction 이라는 '상태 변수'를 둬서 좌/우 방향 전환을 구현.
 *
 *  응용 아이디어:
 *   ▸ 생성자에서 speed 를 받아 FastEnemy, SlowEnemy 로 다양화
 *   ▸ 체력(hp)과 takeDamage() 메서드 추가
 *   ▸ 이동 패턴을 사인파, 원운동 등으로 바꾸기
 *
 * @param minX 왕복 이동의 왼쪽 한계 (보통 0f)
 * @param maxX 왕복 이동의 오른쪽 한계 (보통 worldWidth)
 */
class ExampleEnemy(
    x: Float,
    y: Float,
    private val minY: Float,
    private val maxY: Float,
    private val texture: Texture,
    val scoreValue: Int // 음식마다 자기 점수 기억
) : GameObject(x, y, 80f, 80f) {

    // 이미지 로딩 — src/main/resources/enemy.png.
    //private val texture = Texture(Gdx.files.internal("st1-food-rice.png"))

    private val speed = 250f

    // 현재 진행 방향 — +1 이면 위, -1 이면 아래.
    //   var 로 선언한 이유: 경계에서 반대로 뒤집혀야 하므로 값이 변함.
    private var direction = -1f

    override fun update(delta: Float) {
        // 수직 이동: 속도 × 방향 × 시간
        y += speed * direction * delta
    }

    /**
     * 자신의 이미지를 그린다.
     *   원본은 40x40 이고 width/height 도 40 이라 1:1 로 그려진다.
     *   더 크게 보이게 하려면 width/height 를 늘리면 자동 확대된다.
     */
    //여기에 for문? 배열로 이미지를 묶기?
    override fun draw(batch: SpriteBatch) {
        batch.draw(texture, x, y, width, height)
    }

    override fun dispose() {
        texture.dispose()
    }
}
