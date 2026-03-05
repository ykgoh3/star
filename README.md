# Starcraft (Java Swing Prototype)

자바 Swing으로 만드는 스타크래프트 스타일 RTS 프로토타입입니다.

## 1. 현재 구현 상태

- 유닛 스폰: `Marine`(원거리), `Zergling`(근접)
- 기본 전투:
  - 자동 타겟 탐색
  - 공격 쿨타임/사거리/피격 이펙트
  - 사망 후 시체 타이머 처리
- 이동/명령:
  - 우클릭 이동
  - 우클릭 적 유닛 공격 명령
  - `A` + 좌클릭 Attack-Move
  - `S` 정지
- 선택:
  - 단일 선택(클릭)
  - 박스 선택(드래그)
- 경로 탐색:
  - Grid 기반 A* (`PathFinder`)
  - 간단한 충돌 회피/겹침 해소

## 2. 실행 방법

1. IntelliJ에서 프로젝트 열기
2. `src/starcraft/main/StarMain.java` 실행

기본 실행 창 크기: `800 x 600`

## 3. 조작법

- `좌클릭 드래그`: 아군 유닛 박스 선택
- `우클릭`: 이동 또는 적 클릭 시 공격
- `A` 후 `좌클릭`: Attack-Move
- `S`: 정지

## 4. 코드 구조

- `src/starcraft/main`
  - 진입점 (`StarMain`)
- `src/starcraft/core`
  - 게임 루프/렌더링 (`GamePanel`)
  - 지형 그리드 (`TerrainGrid`)
- `src/starcraft/engine`
  - 입력 처리 (`InputHandler`)
  - 경로 탐색 (`PathFinder`)
  - 유틸리티 (`RenderUtils`, `vectorMath`)
- `src/starcraft/objects`
  - 유닛 공통 추상 클래스 (`Unit`)
  - 상태 로직 (`logic/*`)
  - 유닛 타입 (`units/*`)

## 5. 다음 단계

상세 요구사항과 개발 순서는 아래 문서를 참고하세요.

- `docs/STARCRAFT_MVP_SPEC.md`
- `docs/DEVELOPMENT_ROADMAP.md`
