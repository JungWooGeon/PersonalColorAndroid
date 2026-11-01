package com.gun.personalcolor.feature.settings

internal data class LegalDocument(
    val title: String,
    val effectiveDate: String,
    val intro: String,
    val sections: List<Section>,
    val webUrl: String,
) {
    /** Paragraphs are shown first, then bullets. Headings are numbered on screen. */
    data class Section(val heading: String, val paragraphs: List<String> = emptyList(), val bullets: List<String> = emptyList())
}

internal const val CONTACT_EMAIL = "wjddnrjs123@gmail.com"

internal fun legalDocument(doc: LegalDoc) = if (doc == LegalDoc.PRIVACY) PrivacyPolicy else Terms

// App version of https://personalcolorotest.netlify.app/privacy.html (website-only section left out).
private val PrivacyPolicy = LegalDocument(
    title = "개인정보처리방침",
    effectiveDate = "2026년 11월 1일",
    webUrl = "https://personalcolorotest.netlify.app/privacy.html",
    intro = "개인 개발자 정우건(이하 “운영자”)은 「개인정보 보호법」 등 관련 법령을 준수하며, 이용자의 개인정보를 최소한으로 처리합니다. " +
        "본 방침은 “나의 컬러는? - 퍼스널컬러” 앱(v2.0 이상, 이하 “앱”)과 웹사이트(personalcolorotest.netlify.app)에 적용됩니다.",
    sections = listOf(
        LegalDocument.Section(
            "회원가입 및 직접 수집하는 개인정보",
            paragraphs = listOf(
                "앱은 회원가입 없이 이용할 수 있으며, 운영자는 이름, 연락처, 이메일 등 이용자를 식별하는 정보를 직접 수집하지 않습니다. " +
                    "운영자는 별도의 서버를 운영하여 이용자 정보를 저장하지 않습니다.",
            ),
        ),
        LegalDocument.Section(
            "얼굴 사진 및 진단 결과",
            bullets = listOf(
                "이용자가 촬영하거나 선택한 얼굴 사진은 Google MediaPipe를 이용해 기기 안에서만(온디바이스) 분석되며, 서버로 전송되거나 저장되지 않습니다.",
                "앱은 사진 원본을 별도로 보관하지 않습니다.",
                "진단 결과(퍼스널컬러 톤 이름, 진단 날짜)만 기기 내부 저장소에 저장되며, 운영자는 이에 접근할 수 없습니다. 앱을 삭제하면 함께 삭제됩니다.",
                "내 옷장(저장한 색 값·이름·종류), 즐겨찾기, 설정도 기기 내부에만 저장되며 옷 사진은 저장하지 않습니다.",
                "카메라 및 사진 접근 권한은 진단 기능 이용 시에만 사용되며, 기기 설정에서 언제든지 철회할 수 있습니다.",
            ),
        ),
        LegalDocument.Section(
            "제3자 SDK를 통한 정보 처리",
            paragraphs = listOf("앱은 광고 제공과 결제를 위해 아래 Google 서비스를 사용하며, 해당 서비스가 자동으로 정보를 수집할 수 있습니다."),
            bullets = listOf(
                "Google AdMob: 광고 ID, 기기 정보(모델, OS 버전 등), IP 주소 기반 대략적 위치, 광고 상호작용 정보 — 광고 제공·맞춤화·성과 측정, 부정 광고 방지",
                "Google Play 결제: 구매 내역(상품, 구매 토큰) — \"프리미엄\" 인앱 상품 구매 확인 및 복원 (결제 수단 정보는 Google이 처리하며 운영자에게 제공되지 않아요)",
                "광고 개인정보 동의는 Google UMP(User Messaging Platform)를 통해 관리되며, 앱 설정의 “개인정보 옵션” 메뉴에서 언제든지 다시 설정할 수 있습니다.",
                "기기 설정(Google > 광고)에서 광고 ID를 재설정하거나 삭제하여 맞춤 광고를 제한할 수 있습니다.",
                "카카오톡 공유(Kakao SDK): 이용자가 카카오톡 공유를 선택한 경우 공유할 내용(톤 이름, 소개 문구, 이미지 주소)이 카카오에 전달되며, 카카오의 개인정보처리방침이 적용됩니다.",
                "관련 정책: Google 개인정보처리방침(policies.google.com/privacy), AdMob 개인정보 안내",
            ),
        ),
        LegalDocument.Section(
            "위치 정보 (오늘의 코디 날씨)",
            bullets = listOf(
                "“오늘의 코디”에서 이용자가 위치 권한을 허용한 경우에만, 기기의 대략적인 위치(동네 수준)를 사용해 오늘 날씨를 조회합니다.",
                "날씨 조회를 위해 소수점 둘째 자리로 낮춘 좌표가 날씨 정보 제공자(Open-Meteo, open-meteo.com)에 전송됩니다. 동네 이름 변환은 기기의 위치 서비스로 처리합니다.",
                "운영자는 위치 정보를 수집·저장하지 않으며, 최근 날씨 결과만 기기에 최대 6시간 보관합니다. 위치 권한은 기기 설정에서 언제든 거부할 수 있고, 거부해도 계절 기준으로 코디를 추천합니다.",
            ),
        ),
        LegalDocument.Section(
            "보유 기간 및 파기",
            bullets = listOf(
                "운영자는 이용자 개인정보를 서버에 보관하지 않습니다.",
                "기기에 저장된 진단 결과는 이용자가 앱 데이터를 삭제하거나 앱을 삭제할 때 즉시 파기됩니다. 앱 설정에서 진단 기록만 삭제할 수도 있습니다.",
                "제3자 SDK가 수집한 정보는 각 서비스 제공자(Google)의 정책에 따른 기간 동안 보관 후 파기됩니다.",
            ),
        ),
        LegalDocument.Section(
            "이용자의 권리와 행사 방법",
            paragraphs = listOf(
                "이용자는 언제든지 개인정보 열람, 정정, 삭제, 처리정지를 요구할 수 있습니다. 운영자가 보관하는 정보는 없으므로, 기기 내 정보는 앱 데이터 삭제 또는 앱 삭제로, " +
                    "광고 관련 정보는 앱의 “개인정보 옵션” 및 기기 광고 설정으로 직접 관리할 수 있습니다. 그 밖의 요청은 아래 문의처로 연락해 주시면 지체 없이 처리하겠습니다.",
            ),
        ),
        LegalDocument.Section(
            "아동의 개인정보",
            paragraphs = listOf(
                "앱은 만 14세 미만 아동을 대상으로 하지 않으며, 아동의 개인정보를 의도적으로 수집하지 않습니다. 만 14세 미만 아동의 정보가 처리된 사실을 알게 된 경우 " +
                    "법정대리인은 아래 문의처로 연락해 주시기 바랍니다. 확인 즉시 필요한 조치를 하겠습니다.",
            ),
        ),
        LegalDocument.Section(
            "안전성 확보 조치",
            bullets = listOf(
                "얼굴 사진은 기기 안에서만 처리하여 외부 전송을 원천적으로 차단합니다.",
                "수집 정보를 최소화하고, 제3자 SDK와의 통신은 암호화(HTTPS)된 채널로 이루어집니다.",
                "진단 결과는 앱 전용 내부 저장소에 저장되어 다른 앱이 접근할 수 없습니다.",
            ),
        ),
        LegalDocument.Section(
            "개인정보 보호책임자 및 문의",
            paragraphs = listOf(
                "성명: 정우건 (개인 개발자)\n이메일: $CONTACT_EMAIL",
                "개인정보 침해에 대한 신고·상담은 개인정보침해신고센터(privacy.kisa.or.kr, 국번없이 118), 개인정보분쟁조정위원회(www.kopico.go.kr, 1833-6972)에 문의할 수 있습니다.",
            ),
        ),
        LegalDocument.Section(
            "방침의 변경",
            paragraphs = listOf(
                "본 방침의 내용이 변경되는 경우 시행 7일 전부터 앱 또는 웹사이트를 통해 고지합니다. 이용자 권리에 중대한 변경이 있는 경우 30일 전에 고지합니다.",
                "공고일자: 2026년 11월 1일 / 시행일자: 2026년 11월 1일",
            ),
        ),
    ),
)

// Same text as https://personalcolorotest.netlify.app/terms.html — keep both in sync.
private val Terms = LegalDocument(
    title = "이용약관",
    effectiveDate = "2026년 11월 1일",
    webUrl = "https://personalcolorotest.netlify.app/terms.html",
    intro = "본 약관은 개인 개발자 정우건(이하 “운영자”)이 제공하는 “나의 컬러는? - 퍼스널컬러” 앱 및 관련 서비스의 이용 조건을 정합니다.",
    sections = listOf(
        LegalDocument.Section(
            "목적",
            paragraphs = listOf("본 약관은 운영자가 제공하는 서비스를 이용함에 있어 운영자와 이용자의 권리, 의무 및 책임 사항, 기타 필요한 사항을 규정함을 목적으로 합니다."),
        ),
        LegalDocument.Section(
            "정의",
            bullets = listOf(
                "“서비스”란 운영자가 제공하는 “나의 컬러는? - 퍼스널컬러” Android 앱(이하 “앱”) 및 웹사이트(personalcolorotest.netlify.app)를 통해 제공되는 모든 기능을 말합니다.",
                "“이용자”란 본 약관에 따라 서비스를 이용하는 모든 사람을 말합니다. 서비스는 회원가입 없이 이용할 수 있습니다.",
                "“진단 결과”란 서비스가 사진 분석 또는 문항 응답을 바탕으로 제시하는 퍼스널컬러 톤과 관련 정보를 말합니다.",
            ),
        ),
        LegalDocument.Section(
            "약관의 게시와 변경",
            bullets = listOf(
                "운영자는 본 약관을 앱 설정 화면과 웹사이트에 게시합니다.",
                "운영자는 관련 법령을 위반하지 않는 범위에서 약관을 변경할 수 있으며, 변경 시 시행일과 변경 사유를 명시하여 시행 7일 전부터 고지합니다. 이용자에게 불리한 변경은 30일 전부터 고지합니다.",
                "이용자가 변경된 약관에 동의하지 않는 경우 서비스 이용을 중단하고 앱을 삭제할 수 있습니다. 시행일 이후에도 서비스를 계속 이용하면 변경된 약관에 동의한 것으로 봅니다.",
            ),
        ),
        LegalDocument.Section(
            "서비스의 내용",
            paragraphs = listOf("운영자는 다음 서비스를 제공합니다."),
            bullets = listOf(
                "사진 기반 퍼스널컬러 진단: 얼굴 사진의 피부·눈동자·입술·머리카락 색을 기기 안에서 분석해 12톤 중 어울리는 톤을 제시",
                "셀프 진단: 문항에 답하여 퍼스널컬러를 추정",
                "컬러 도감: 12톤별 베스트·워스트 컬러, 메이크업·헤어·패션 가이드",
                "드레이핑: 사진 위에 여러 색을 대어 보며 비교하는 기능",
                "컬러 체크: 쇼핑 등에서 고른 색이 나의 톤과 어울리는지 확인하는 기능",
                "진단 기록, 즐겨찾기 등 그 밖에 운영자가 정하는 기능",
            ),
        ),
        LegalDocument.Section(
            "서비스 이용",
            bullets = listOf(
                "서비스의 기본 기능은 무료로 제공되며, 회원가입이나 로그인이 필요하지 않습니다.",
                "서비스에는 광고가 포함되어 있으며, 운영자는 광고 수익으로 서비스를 운영합니다.",
                "진단 기록 등 이용 데이터는 이용자의 기기에만 저장되므로, 앱을 삭제하거나 기기를 변경하면 복구할 수 없습니다.",
            ),
        ),
        LegalDocument.Section(
            "진단 결과의 성격",
            bullets = listOf(
                "진단 결과는 참고용 정보이며, 정확성이나 특정 목적에의 적합성을 보증하지 않습니다.",
                "사진 진단 결과는 조명, 카메라 성능과 설정, 화장, 필터, 화면 밝기·색감 등에 따라 달라질 수 있습니다.",
                "진단 결과는 전문 컬러 컨설턴트의 대면 진단이나 의학적·전문적 조언을 대체하지 않습니다.",
            ),
        ),
        LegalDocument.Section(
            "광고 및 유료 상품",
            bullets = listOf(
                "운영자는 서비스 화면에 Google AdMob 등 제3자 광고를 게재할 수 있습니다.",
                "광고의 내용과 광고를 통해 연결되는 상품·서비스에 대한 책임은 해당 광고주에게 있으며, 이용자와 광고주 사이의 거래에 운영자는 관여하지 않습니다.",
                "맞춤 광고 동의는 앱 설정의 “개인정보 옵션”과 기기 광고 설정에서 관리할 수 있습니다.",
                "일부 기능(사진 진단, 립 체험, 리포트 저장 등)은 이용자가 원하는 경우 보상형 광고를 시청한 뒤 이용할 수 있으며, 광고를 불러올 수 없을 때에도 기능 이용은 제한되지 않습니다.",
                "이용자는 Google Play를 통해 유료 상품 “광고 제거”(1회 구매)를 구입하여 광고 없이 서비스를 이용할 수 있습니다. 같은 Google 계정이라면 앱 설정의 “구매 복원”으로 다시 적용할 수 있습니다.",
                "유료 상품의 결제·청약철회·환불은 「전자상거래 등에서의 소비자보호에 관한 법률」 등 관계 법령과 Google Play 환불 정책에 따릅니다.",
            ),
        ),
        LegalDocument.Section(
            "이용자의 의무",
            paragraphs = listOf("이용자는 다음 행위를 해서는 안 됩니다."),
            bullets = listOf(
                "타인의 사진을 본인의 동의 없이 촬영·사용하거나, 진단 결과를 이용해 타인의 명예를 훼손하거나 비방하는 행위",
                "서비스를 역설계, 변조, 복제하거나 비정상적인 방법으로 이용하는 행위",
                "광고를 부정하게 클릭하거나 광고 노출을 조작하는 행위",
                "그 밖에 관련 법령이나 본 약관을 위반하는 행위",
            ),
        ),
        LegalDocument.Section(
            "지식재산권",
            bullets = listOf(
                "서비스의 디자인, 텍스트, 컬러 가이드 등 콘텐츠와 소프트웨어에 대한 권리는 운영자 또는 정당한 권리자에게 있습니다.",
                "이용자는 운영자의 사전 동의 없이 서비스의 콘텐츠를 영리 목적으로 복제, 배포, 2차 가공할 수 없습니다. 단, 개인적인 진단 결과 화면을 공유하는 것은 자유롭게 할 수 있습니다.",
                "서비스에 포함된 오픈소스 소프트웨어는 각 라이선스를 따르며, 앱의 “오픈소스 라이선스” 메뉴에서 확인할 수 있습니다.",
            ),
        ),
        LegalDocument.Section(
            "서비스의 변경·중단",
            bullets = listOf(
                "운영자는 운영상·기술상 필요에 따라 서비스의 전부 또는 일부를 변경하거나 중단할 수 있습니다.",
                "서비스를 종료하는 경우 가능한 한 사전에 앱 또는 웹사이트를 통해 고지합니다.",
                "무료 기능의 변경·중단에 대해 운영자는 관련 법령에 특별한 규정이 없는 한 별도의 보상을 하지 않습니다. 유료 상품 구매자에 대해서는 관계 법령에 따라 처리합니다.",
            ),
        ),
        LegalDocument.Section(
            "책임의 제한",
            bullets = listOf(
                "운영자는 천재지변, 기기·통신 장애 등 운영자의 귀책사유 없이 발생한 서비스 장애에 대해 책임을 지지 않습니다.",
                "운영자는 이용자가 진단 결과를 참고하여 내린 화장품·의류 구매 등의 결정과 그 결과에 대해 책임을 지지 않습니다.",
                "운영자는 이용자의 기기에 저장된 진단 기록 등이 앱 삭제, 기기 변경 등으로 사라진 데 대해 책임을 지지 않습니다.",
                "다만 운영자의 고의 또는 중대한 과실로 인한 손해에 대해서는 관련 법령에 따라 책임을 집니다.",
            ),
        ),
        LegalDocument.Section(
            "준거법 및 관할",
            paragraphs = listOf(
                "본 약관과 서비스 이용에 관한 사항은 대한민국 법을 준거법으로 합니다. 서비스 이용과 관련하여 분쟁이 발생한 경우 「민사소송법」에 따른 관할 법원을 관할 법원으로 합니다.",
            ),
        ),
        LegalDocument.Section(
            "문의",
            paragraphs = listOf("운영자: 정우건 (개인 개발자)\n이메일: $CONTACT_EMAIL"),
        ),
        LegalDocument.Section(
            "부칙",
            paragraphs = listOf("본 약관은 2026년 11월 1일부터 시행합니다."),
        ),
    ),
)
