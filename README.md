# 나의 컬러는? - 퍼스널컬러 (Android) v2.0

사진 한 장으로 12톤 퍼스널컬러를 진단하고, 내 톤에 맞는 색·메이크업·코디를 매일 보여 주는 앱.
앱의 중심은 **퍼스널컬러**이고, 립·옷·코디는 진단된 톤에서 파생되는 콘텐츠다.
얼굴 분석은 전부 기기 안에서 처리하며 사진은 서버로 보내지 않는다.

- 브랜드 캐릭터: **틴티(Tinty)**. 진단 결과에서 내 톤 색으로 물드는 물감 방울.
- 웹/공유 페이지: https://tintycolor.com (저장소: `JungWooGeon/personalColor`)
- 대상: 10대 후반~20대 초중반 여성. 디자인은 "감성 룩북" 무드(필름톤·창빛·리넨).

## 구조 (멀티 모듈, Hilt, Compose)
```
app/                 Application, MainActivity, 앱 셸(하단 탭/태블릿 레일), 내비게이션(PcApp, PcNavHost),
                     딥링크(DeepLink), 위젯 데이터 바인딩(WidgetBinding), 강제 업데이트(ForcedUpdate)
build-logic/         Gradle 컨벤션 플러그인 (pc.android.feature, pc.hilt …)
core/
  model/             데이터 모델 (Tone, HistoryEntry, UserPreferences, 옷 종류 CLOTHING_KINDS 93종 …)
  analysis/          순수 Kotlin 색 분석 엔진 (Lab 변환, 화이트밸런스, 12톤 분류)
  vision/            MediaPipe 얼굴 랜드마크·사람 분리, 사진 로딩, ML Kit 옷 배경 제거(GarmentCutter)
  data/              콘텐츠(assets JSON), 진단 기록, 설정, 옷장 (DataStore, 읽기/쓰기 실패 시 안전하게 처리)
  domain/            유스케이스: 사진 진단, 셀프 진단, 결과 공유 세션
  designsystem/      테마, 공통 컴포넌트, 틴티 캐릭터·소품(Tinty, TintyProps), Pretendard·나눔명조 폰트
  ui/                톤 공통 UI, 옷 그림(GarmentArt), 리포트·배경화면 렌더러,
                     mood/ 감성 장면(SceneBed 침대, SceneWall 옷걸이 벽), MoodCopy, MoodTile
  ads/               AdMob + UMP 동의 + Play Billing(premium, 구매 복원)
  analytics/         이벤트 기록 (Logcat만, 외부 전송 없음)
  weather/           대략적 위치 + Open-Meteo 날씨 (키 불필요, 좌표 저장 안 함)
  share/             카카오톡 공유, 공용 이미지 공유(ImageShare)
feature/
  onboarding  home  analyze  result  drape(컬러 드레이핑·립 체험)  quiz(셀프 진단)
  tones (도감·상세·비교)  colormatch (컬러 체크·내 옷장)  learn (가이드 14편)
  my (마이·기록·배경화면·움직이는 틴티 배경화면)  settings (설정·약관·라이선스·프리미엄)
  coordi (오늘의 코디: OutfitEngine·감성 룩북 히어로·매일 알림)
  widget (홈 화면 "오늘의 룩" 위젯 1x1·2x2·4x2)
tools/
  model/             진단 모델 근거 자료·학습 스크립트
  garment_art/       옷 그림 93종 원본(SVG 생성 코드)과 앱 에셋 변환기(export.py)
  mood/              위젯·코디 감성 장면 생성기와 참고 렌더
store/               Play 스토어 등록 자료 (설명, 데이터 보안 답안, 체크리스트, 아이콘·그래픽·스크린샷)
```
- 기능 모듈끼리는 서로 의존하지 않는다. 화면 간 이동은 `app/.../navigation/PcNavHost.kt`에서만 연결한다.
- 콘텐츠는 `core/data/src/main/assets/`의 `tones.json`, `quiz.json`, `articles.json`만 고치면 된다 (형식은 단위 테스트가 검사).

## 주요 기능 메모
- **진단**: 피부 4부위·머리카락·눈동자·입술 측정, 흰 종이/흰자위 화이트밸런스 → 온디바이스 모델(`ToneModel.kt`). 결과·기록에 일치도(%) 같은 수치는 보여 주지 않는다.
- **옷 그림**: `core/ui/src/main/assets/garments/<종류>_{n,d}.webp`. 색을 런타임에 입힌다(`GarmentArt`). 새 종류를 추가하려면 `tools/garment_art/garments/<key>.py`를 만들고 `python3 export.py ../../core/ui/src/main/assets/garments <key>` 실행. 모든 종류에 에셋이 있는지는 테스트가 검사한다.
- **오늘의 코디(OutfitEngine)**: 뉴트럴 베이스 + 톤 포인트 색 1개, 신발·가방은 뉴트럴, 비 오는 날 어두운 하의, 입는 순서(아우터→상의→하의→신발→가방→소품). 규칙은 `OutfitEngineTest`의 전체 조합 테스트로 고정.
- **감성 장면**: `feature/widget/src/main/assets/mood/`의 미리 렌더한 배경·빛 지도 + `A_layout.json`/`B_layout.json`. 위젯 2x2·1x1은 침대(A), 4x2는 옷걸이 벽(B), 앱 코디 화면·공유 이미지는 A의 4:5 hero. 위젯은 배경만 35% 비침(`SCENE_ALPHA`), 옷·글씨는 불투명.
- **위젯 잠금**: 무료 체험(7일) 또는 프리미엄 또는 오늘 광고 시청 시 열림. 잠겨 있으면 실제 코디를 그리지 않는다.
- **움직이는 배경화면**: 틴티 기본. 밝은 배경이면 런처에 어두운 글씨 힌트를 준다.
- **강제 업데이트**: Play에 새 버전이 있으면 항상 IMMEDIATE 업데이트. 사용자가 닫으면 앱 종료, Play 오류면 계속 사용 가능. → **새 버전을 올리면 v2.0 이상 모든 사용자가 바로 업데이트해야 하므로 작은 수정은 모아서 출시.**

## 빌드
`local.properties`에 아래 값을 넣는다. 디버그 빌드는 AdMob 테스트 ID를 자동으로 쓰고, 릴리스 빌드는 값이 없으면 실패한다.
```
reward_ad_unit_id=ca-app-pub-2864408286392678/XXXXXXXXXX
interstitial_ad_unit_id=ca-app-pub-2864408286392678/XXXXXXXXXX
native_ad_unit_id=ca-app-pub-2864408286392678/XXXXXXXXXX
# 선택: 없으면 카카오톡 공유 대신 일반 공유
kakao_native_app_key=XXXXXXXXXXXXXXXX
```
- 출시 파일: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` (서명 전).
- 서명: 업로드 키는 Expo(EAS)가 만든 JKS. 키 파일과 비밀번호는 `npx eas-cli credentials -p android` → Keystore → Download existing keystore 로 받을 수 있다(프로젝트 `@jungwoogeon/personalcolorapp`). 비밀번호는 저장소에 넣지 말 것.
  ```
  jarsigner -keystore <업로드키.jks> -sigalg SHA256withRSA -digestalg SHA-256 app-release.aab 'QGp1bmd3b29nZW9uL3BlcnNvbmFsY29sb3JhcHA='
  ```
- Play 앱 서명 사용 중. 카카오 키 해시는 Play **앱 서명 키** 기준(업로드 키 아님).

## 광고·결제 정책
- 네이티브: 화면당 최대 1개, 콘텐츠 카드 모양 ("광고" 표시). 배너 없음.
- 보상형: AI 사진 진단 전 선택형(성공 시에만 소모), 립 체험·리포트는 앱 종료 전까지, 오늘의 코디(체험 7일 후)는 자정까지 해제. 광고가 없으면 그냥 진행.
- 전면: 결과 직전 1개, 다른 광고 직후 90초는 생략. 화면이 닫히는 중이면 광고를 띄우지 않는다.
- "프리미엄" 1회 결제(Play 일회성 제품 ID `premium`): 광고 제거 + 오늘의 코디 무제한 + 립 체험·리포트. 구매 복원은 결과(복원/없음/연결 실패)를 그대로 안내.

## 주의 (꼭 읽기)
- **Hilt 코드 생성 누락 방지**: KSP를 여러 모듈에서 동시에 돌리면 가끔 Hilt 코드가 비어 나오고 그게 빌드 캐시에 남아 "Cannot create an instance of …ViewModel" 크래시가 났다. `build-logic`의 `pc.hilt`에서 KSP를 한 번에 하나씩 돌리고 캐시하지 않게 했다. 또 `verifyHiltViewModels{Debug,Release}`가 패키징 전에 모든 `@HiltViewModel`이 생성 코드에 있는지 검사해 빠지면 빌드를 실패시킨다. 이 두 장치를 지우지 말 것.
- **MediaPipe는 `0.10.32`로 고정**. 1.2.0은 얼굴 인식까지 SIGILL. SME(최신 Arm 기능)를 알리는 기기(Apple M4/M5 에뮬레이터 등)에서는 사람 분리(PersonSegmenter)를 건너뛰고 케이프만 그린다. SME2 실기기에서 검증되면 `PersonSegmenter.kt`의 조건을 풀 수 있다.
- R8 keep 규칙(`app/proguard-rules.pro`)을 지우면 릴리스에서 분석·카카오 공유가 실패한다(MediaPipe, Flogger, Room, Kakao Retrofit 규칙).
- 내비게이션 경로 인자로 enum을 쓰면 `@Keep`을 붙인다 (예: `LegalDoc`).
- 16KB 페이지: 네이티브 라이브러리 모두 16KB 정렬 확인됨. 네이티브가 든 SDK를 추가하면 다시 확인.
- 촬영 임시 사진(`cacheDir/guided_*.jpg` 등)은 분석·불러오기 후 바로 지운다(개인정보처리방침과 일치).
- 개인정보처리방침은 웹(`tintycolor.com/privacy.html`)과 앱(`feature/settings/.../LegalDocuments.kt`)을 항상 같이 고친다.
