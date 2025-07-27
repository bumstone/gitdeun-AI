package com.hyetaekon.hyetaekon.chatbot.controller;

import com.hyetaekon.hyetaekon.chatbot.dto.ChatbotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    @GetMapping
    public ResponseEntity<ChatbotDto> getAnswer(@RequestParam String question) {

        switch (question) {

            // ✅ 초기 인사
            case "처음":
                return ok("처음", "안녕하세요! 혜택온 챗봇입니다. 무엇을 도와드릴까요?",
                        List.of("혜택온 사이트 소개", "포인트 제도 설명",
                                "커뮤니티 이용 가이드", "신고 및 제재 기준"));

            // ✅ 혜택온 사이트 소개
            case "혜택온 사이트 소개":
                return ok(question, "혜택온은 복지 정보를 확인하고, 커뮤니티에서 의견을 나눌 수 있는 플랫폼입니다.\n궁금한 주제를 선택해주세요.",
                        List.of("복지 서비스", "가구 형태 기준"));

            case "복지 서비스":
                return ok(question, "복지 서비스는 주거, 보육, 교육, 고용, 의료, 돌봄 등 여러 분야로 구성되어 있어요.", null);

            case "가구 형태 기준":
                return ok(question, "1인가구, 다자녀 가구, 청년가구, 노인가구, 장애인가구 등 다양한 형태로 구분돼요.", null);

            // ✅ 포인트 제도
            case "포인트 제도 설명":
                return ok(question, "혜택온에서는 활동에 따라 포인트가 부여되며, 등급이 상승합니다. 자세한 내용을 선택해주세요.",
                        List.of("포인트 획득 방법", "등급별 혜택"));

            case "포인트 획득 방법":
                return ok(question,
                        "- 회원가입 후 인사 게시판 글 작성 시 100점\n" +
                                "- 일반 게시판 글 작성 시 20점\n" +
                                "- 게시글에 답변 작성 시 10점 (한 게시글당 최대 50점)\n" +
                                "- 답변이 채택되면 추가 50점\n" +
                                "※ 무의미한 답변 작성 시 관리자가 포인트 감점 또는 등급 하락 조치할 수 있습니다.",
                        null);

            case "등급별 혜택":
                return ok(question,
                        "포인트 등급은 아래와 같이 구분됩니다:\n" +
                                "- 0점: 물음표\n" +
                                "- 100점: 알\n" +
                                "- 300점: 병아리\n" +
                                "- 500점: 닭\n" +
                                "- 700점: 독수리\n" +
                                "- 1000점 이상: 구름",
                        null);

            // ✅ 커뮤니티 이용 가이드
            case "커뮤니티 이용 가이드":
                return ok(question, "커뮤니티에서는 다음과 같은 기능들을 사용할 수 있어요. 궁금한 항목을 선택해주세요.",
                        List.of("게시글 작성 방법", "댓글/대댓글 기능", "좋아요 및 공감", "북마크 사용법", "댓글 채택"));

            case "게시글 작성 방법":
                return ok(question,
                        "게시글은 제목(20자 이내), 내용(500자 이내)으로 작성할 수 있으며,\n" +
                                "이미지 첨부(5MB), 태그, 관련 링크 추가도 가능합니다.\n" + "단, 규칙에 어긋나는 표현은 삭제 또는 제재될 수 있어요.",
                        null);

            case "댓글/대댓글 기능":
                return ok(question,
                        "게시글에는 댓글을 작성할 수 있고, 댓글에는 대댓글도 작성 가능합니다.\n" +
                                "단, 규칙에 어긋나는 표현은 삭제 또는 제재될 수 있어요.",
                        null);

            case "좋아요 및 공감":
                return ok(question,
                        "게시글과 댓글에는 좋아요 버튼이 있습니다.\n" +
                                "유저 활동 점수에 반영되진 않지만 인기 콘텐츠를 판단하는 기준이 돼요.",
                        null);

            case "북마크 사용법":
                return ok(question,
                        "관심 있는 게시글 우측 상단의 북마크 아이콘을 클릭하면\n" +
                                "마이페이지에서 따로 모아볼 수 있습니다.",
                        null);

            case "댓글 채택":
                return ok(question,
                        "질문 게시판에서 답변이 마음에 들 경우 '채택' 버튼을 눌러 채택할 수 있으며,\n" +
                                "채택된 유저는 50포인트를 추가로 획득합니다.",
                        null);

            // ✅ 신고 및 제재 기준
            case "신고 및 제재 기준":
                return ok(question, "다음 항목 중 신고 사유를 선택하면 제재 기준을 안내해 드릴게요.",
                        List.of("음란/선정적인 내용", "스팸/광고", "욕설/비하", "폭력성/위협",
                                "개인 정보 노출", "허위 정보", "도배/복제/악성 행위", "기타"));

            case "음란/선정적인 내용":
                return ok(question, "1차: 10일 정지 / 2차: 30일 정지 / 3차: 영구정지", null);

            case "스팸/광고":
                return ok(question, "스팸, 광고, 무단 홍보 게시글은 3일 정지 조치됩니다.", null);

            case "욕설/비하":
                return ok(question, "욕설, 인신공격, 비하 발언은 1일 정지 처리됩니다.", null);

            case "폭력성/위협":
                return ok(question, "폭력적이거나 위협적인 내용은 10일~영구정지까지 조치됩니다.", null);

            case "개인 정보 노출":
                return ok(question, "타인의 개인정보 노출은 10일~영구정지 대상입니다.", null);

            case "허위 정보":
                return ok(question, "허위사실 유포 시 3일 정지 조치됩니다.", null);

            case "도배/복제/악성 행위":
                return ok(question, "같은 내용 반복, 복사/붙여넣기 등은 1일 정지입니다.", null);

            case "기타":
                return ok(question, "사례별로 관리자가 판단하여 별도 조치가 적용됩니다.", null);

            // ✅ 기본 응답
            default:
                return ok(question, "죄송해요, 해당 질문에 대한 답변을 찾을 수 없어요.", null);
        }
    }

    // ✅ 공통 응답 생성 메서드
    private ResponseEntity<ChatbotDto> ok(String question, String answer, List<String> options) {
        return ResponseEntity.ok(ChatbotDto.builder()
                .question(question)
                .answer(answer)
                .options(options)
                .build());
    }
}
