# 나의 컬러는? - 퍼스널컬러 (Android) v2.0

사진 한 장으로 12톤 퍼스널컬러를 진단하는 앱. 얼굴 분석은 전부 기기 안에서 처리하고, 사진은 저장·전송하지 않는다.

## 구조 (멀티 모듈, Hilt)
```
app/                 Application, MainActivity, 앱 셸(하단 탭/태블릿 레일), 전체 내비게이션 그래프
build-logic/         Gradle 컨벤션 플러그인 (pc.android.feature 등)
core/
  model/             데이터 모델 (Tone, HistoryEntry, UserPreferences, Article…)
  analysis/          순수 Kotlin 색 분석 엔진 (Lab 변환, 화이트밸런스, 12톤 분류) + 단위 테스트
  vision/            MediaPipe 얼굴 랜드마크 → 부위별 픽셀 추출, 사진 로딩
  data/              저장소: 콘텐츠(assets JSON), 진단 기록, 설정(DataStore)
  domain/            유스케이스: 사진 진단, 셀프 진단, 결과 공유 세션
  designsystem/      테마(라이트/다크), 공통 컴포넌트, 반응형 폭 헬퍼
  ui/                톤 관련 공통 UI (스와치, 톤 카드, 톤 가이드)
  ads/               AdMob + UMP 동의
  analytics/         이벤트 기록 (Logcat만, 외부 전송 없음)
  weather/           현재 위치(대략) + Open-Meteo 오늘 날씨 (키 불필요)
  share/             카카오톡 공유 (키 없으면 일반 공유로 대체)
feature/
  onboarding  home  analyze  result  drape  quiz
  tones (도감·상세·비교)  colormatch (컬러 체크·내 옷장)  learn (가이드)  my (마이·기록·배경화면)  settings (설정·약관·라이선스·광고 제거)
  widget (홈 화면 "오늘의 컬러" 위젯)  coordi (오늘의 코디·매일 알림)
```
- 기능 모듈끼리는 서로 의존하지 않는다. 화면 간 이동은 `app/.../navigation/PcNavHost.kt`에서만 연결한다.
- 콘텐츠는 코드 수정 없이 `core/data/src/main/assets/`의 `tones.json`, `quiz.json`, `articles.json`만 고치면 된다.
- 진단 엔진: 측정(피부 4부위·머리카락·눈동자·입술, 흰 종이/흰자위 화이트밸런스) → 온디바이스 MLP(`ToneModel.kt`). 근거 자료·학습 스크립트·재학습 방법은 `tools/model/` 참고.

## 빌드
`local.properties`에 아래 값을 넣는다. 디버그 빌드는 AdMob 테스트 ID를 자동으로 쓰고, 릴리스 빌드는 값이 없으면 실패한다.
```
reward_ad_unit_id=ca-app-pub-2864408286392678/XXXXXXXXXX
interstitial_ad_unit_id=ca-app-pub-2864408286392678/XXXXXXXXXX
native_ad_unit_id=ca-app-pub-2864408286392678/XXXXXXXXXX
# 선택: 없으면 카카오톡 공유 대신 일반 공유
kakao_native_app_key=XXXXXXXXXXXXXXXX
```

## 광고 정책 (AdMob "콘텐츠보다 광고가 많음" 제한 대응)
- 네이티브: 화면당 최대 1개, 콘텐츠 카드 모양으로 콘텐츠 사이에 배치 ("광고" 표시). 홈 하단, 도감 중간, 가이드 목록 중간·글 끝, 결과 하단.
- 보상형: AI 사진 진단 전 선택형. 진단 성공 시에만 소모(실패한 재촬영은 무료). 립 체험·리포트는 앱 종료 전까지, 오늘의 코디(체험 7일 후)는 자정까지 해제. 광고가 없으면 그냥 진행.
- 전면: 매 결과 직전 1개. 다른 전면/보상형 광고 직후(90초)에는 생략해 연속 노출 방지. AdMob 콘솔에서 빈도 제한도 설정할 것.
- 배너 없음. 기능은 광고를 기다리지 않는다.
- "프리미엄" 1회 결제(Play Console 인앱 상품 ID `premium`): 광고 제거 + 오늘의 코디 무제한 + 립 체험·리포트.

## 주의
- MediaPipe는 `0.10.32`로 고정. 0.10.33 이상은 Apple M4/M5 Mac의 에뮬레이터에서 SIGILL로 죽는다. 실기기 영향은 미확인.
- R8 keep 규칙(`app/proguard-rules.pro`)을 지우면 릴리스에서 분석이 실패한다. MediaPipe, Flogger, Room 규칙이 들어 있다.
- 내비게이션 경로 인자로 enum을 쓰면 `@Keep`을 붙여야 릴리스에서 안 죽는다 (예: `LegalDoc`).
