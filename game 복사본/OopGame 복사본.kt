package com.oop.game

import com.badlogic.gdx.Game
import com.oop.game.example.ExampleWorld

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  이 프로젝트의 '게임 본체' — LibGDX 의 Game 을 상속해서 만든다.
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 *  Game 은 LibGDX 가 제공하는 '게임 앱의 최상위 껍데기' 클래스다.
 *    - create()   : 앱 시작 시 한 번 호출 (초기화 자리)
 *    - render()   : 매 프레임 호출 (내부에서 현재 Screen.render 를 대신 돌려줌)
 *    - setScreen(): 현재 Screen 을 바꾸는 메서드
 *
 *  이 중 create() 는 추상 메서드 (ApplicationListener 인터페이스 상속).
 *  즉 Game 을 상속하는 순간 반드시 create() 를 구현해야 한다.
 *
 *  (안드로이드의 Activity.onCreate() 와 완전히 같은 패턴이다.
 *   onCreate() 안에서 setContentView(...) 로 첫 화면을 붙이듯,
 *   여기서는 create() 안에서 setScreen(...) 으로 첫 Screen 을 붙인다.)
 *
 *  왜 이 파일이 core 에 있나?
 *    OopGame 은 특정 OS 에 의존하는 코드가 전혀 없다 (LibGDX Game 상속뿐).
 *    따라서 데스크톱·안드로이드·iOS 어느 플랫폼에서 띄우든 그대로 쓸 수 있다.
 *    플랫폼별 런처(DesktopLauncher 등)만 따로 두면 된다.
 *
 *  자기 게임을 만들 때 고칠 곳:
 *   ▸ screenWidth / screenHeight   : 창(카메라) 크기
 *   ▸ worldWidth  / worldHeight    : 스크롤 가능한 월드 크기
 *   ▸ create() 안에서 setScreen 에 넘기는 Screen 을 자기 Screen 으로 교체
 */
class OopGame : Game() {

    // 화면(창) 크기 — DesktopLauncher 가 창 크기 설정에도 이 값을 읽어간다.
    //   public(기본)으로 둔 이유: 외부(DesktopLauncher)에서 접근해야 하므로.
    val screenWidth = 1280
    val screenHeight = 960

    // 월드 크기 — 화면의 1.5배. 카메라(WASD)로 탐험 가능한 영역.
    //   이 값은 내부 설정이므로 private.
    private val worldWidth = 1280
    private val worldHeight = 960

    /**
     * LibGDX 가 게임 시작 시 한 번 호출하는 라이프사이클 메서드.
     *
     * 이 함수는 'Gdx.graphics / Gdx.gl / Gdx.files 같은 전역이 모두 준비된 뒤'
     * 호출되므로, Screen 안에서 SpriteBatch / BitmapFont / Texture 같은 LibGDX 자원을
     * 만들어도 안전하다. (생성자에서 미리 Screen 을 만들면 크래시 난다.)
     *
     * 보통 여기서 할 일:
     *   1. 첫 월드(GameWorld 의 자식) 를 만들고
     *   2. setScreen(...) 으로 등록 → 이후 LibGDX 가 매 프레임 그 월드를 렌더
     *
     *  GameWorld 가 LibGDX 의 Screen 인터페이스를 상속하므로 setScreen 인자로 넘길 수 있다.
     */
    override fun create() {
        val firstWorld = ExampleWorld(
            screenWidth = screenWidth.toFloat(),
            screenHeight = screenHeight.toFloat(),
            worldWidth = worldWidth.toFloat(),
            worldHeight = worldHeight.toFloat()
        )
        setScreen(firstWorld)   // 부모 Game 이 제공하는 메서드
    }
}
