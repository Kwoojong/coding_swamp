package com.study.codingswamp.presentation.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.codingswamp.application.auth.token.TokenProvider;
import com.study.codingswamp.domain.member.entity.Member;
import com.study.codingswamp.domain.member.repository.MemberRepository;
import com.study.codingswamp.domain.study.dto.request.ApplyRequest;
import com.study.codingswamp.domain.study.dto.request.StudyRequest;
import com.study.codingswamp.domain.study.entity.*;
import com.study.codingswamp.domain.study.repository.ApplicantRepository;
import com.study.codingswamp.domain.study.repository.ParticipantRepository;
import com.study.codingswamp.domain.study.repository.StudyRepository;
import com.study.codingswamp.util.fixture.dto.study.ApplyRequestFixture;
import com.study.codingswamp.util.fixture.entity.member.MemberFixture;
import com.study.codingswamp.util.fixture.entity.study.ApplicantFixture;
import com.study.codingswamp.util.fixture.entity.study.ParticipantFixture;
import com.study.codingswamp.util.fixture.entity.study.StudyFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
@Transactional
public class StudyControllerDocTest {

    @Autowired
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint(), removeHeaders("Vary"))
                )
                .build();

        jdbcTemplate.update("alter table study auto_increment= ?", 1);
        jdbcTemplate.update("alter table member auto_increment= ?", 1);
    }

    @Test
    @DisplayName("????????? ?????? ????????? ???????????? ?????????????????????.")
    void createStudy() throws Exception {
        // given
        StudyRequest request = StudyRequest.builder()
                .title("???????????????.")
                .description("???????????????.")
                .studyType("STUDY")
                .thumbnail("#000000")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .maxMemberCount(30)
                .tags(List.of("??????1", "??????2"))
                .build();

        Member member = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(member.getId(), member.getRole());

        String json = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/study")
                        .contentType(APPLICATION_JSON)
                        .content(json)
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isCreated())
                .andDo(document("study-create",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        requestFields(
                                fieldWithPath("title").description("??????"),
                                fieldWithPath("description").description("??????"),
                                fieldWithPath("studyType").description("????????? ?????? STUDY or MOGAKKO"),
                                fieldWithPath("thumbnail").description("????????? ????????????"),
                                fieldWithPath("startDate").description("????????? ????????? ?????? (yy-MM-dd)"),
                                fieldWithPath("endDate").description("????????? ????????? ?????? (yy-MM-dd)"),
                                fieldWithPath("maxMemberCount").description("????????? ????????????"),
                                fieldWithPath("tags").description("?????? type(List)")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ?????? ?????? ????????????")
    void getStudyDetail() throws Exception {
        // given
        Member member = memberRepository.save(MemberFixture.create(true));
        Study study = StudyFixture.create(member);
        Participant participant = ParticipantFixture.create(member, study);
        study.initParticipants(participant);
        Member hong = memberRepository.save(MemberFixture.createGithubMember());
        study.addApplicant(ApplicantFixture.create(study, hong));
        studyRepository.save(study);

        // expected
        mockMvc.perform(get("/api/study/{studyId}", study.getId()))
                .andExpect(status().isOk())
                .andDo(document("study-get-detail",
                        pathParameters(
                               parameterWithName("studyId").description("????????? ????????? type(Long)")
                        ),
                        responseFields(
                                fieldWithPath("studyId").description("????????? ?????????"),
                                fieldWithPath("title").description("??????"),
                                fieldWithPath("description").description("??????"),
                                fieldWithPath("studyType").description("????????? ??????"),
                                fieldWithPath("thumbnail").description("????????? ????????????"),
                                fieldWithPath("studyStatus").description("????????? ??????"),
                                fieldWithPath("currentMemberCount").description("?????? ??????"),
                                fieldWithPath("maxMemberCount").description("?????? ??????"),
                                fieldWithPath("startDate").description("????????? ?????????"),
                                fieldWithPath("endDate").description("????????? ?????????"),
                                fieldWithPath("owner").description("???????????? ??????"),
                                fieldWithPath("owner.memberId").description("???????????? memberId"),
                                fieldWithPath("owner.username").description("???????????? ?????????"),
                                fieldWithPath("owner.imageUrl").description("???????????? ?????????"),
                                fieldWithPath("owner.profileUrl").description("???????????? ????????????"),
                                fieldWithPath("owner.participationDate").description("???????????? ?????????"),
                                fieldWithPath("participants").description("????????? ??????"),
                                fieldWithPath("participants[].memberId").description("????????? memberId"),
                                fieldWithPath("participants[].username").description("????????? ?????????"),
                                fieldWithPath("participants[].imageUrl").description("????????? ?????????"),
                                fieldWithPath("participants[].profileUrl").description("????????? ????????????"),
                                fieldWithPath("participants[].participationDate").description("????????? ?????????"),
                                fieldWithPath("applicants").description("????????? ??????"),
                                fieldWithPath("applicants[].memberId").description("????????? memberId"),
                                fieldWithPath("applicants[].username").description("????????? ?????????"),
                                fieldWithPath("applicants[].imageUrl").description("????????? ?????????"),
                                fieldWithPath("applicants[].profileUrl").description("????????? ????????????"),
                                fieldWithPath("applicants[].reasonForApplication").description("?????? ??????"),
                                fieldWithPath("applicants[].applicationDate").description("????????? ?????????"),
                                fieldWithPath("tags").description("?????? ??????"),
                                fieldWithPath("createdAt").description("????????? ?????????")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ???????????? ???????????? ???????????? ????????? ???????????? ??????????????? ??????.")
    void apply() throws Exception {
        // given
        Member member = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(member.getId(), member.getRole());

        Member studyOwner = memberRepository.save(MemberFixture.create(true));
        Study study = StudyFixture.create(studyOwner);
        studyRepository.save(study);

        ApplyRequest applyRequest = ApplyRequestFixture.create();

        String json = objectMapper.writeValueAsString(applyRequest);

        // expected
        mockMvc.perform(patch("/api/study/{studyId}/apply", study.getId())
                        .contentType(APPLICATION_JSON)
                        .content(json)
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isCreated())
                .andDo(document("study-apply",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        pathParameters(
                                parameterWithName("studyId").description("????????? ????????? type(Long)")
                        ),
                        requestFields(
                                fieldWithPath("reasonForApplication").description("?????? ??????/ ?????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ??????????????? ??????????????? ????????? ??? ??????.")
    void approve() throws Exception {
        // given
        Member studyOwner = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(studyOwner.getId(), studyOwner.getRole());
        Study study = StudyFixture.create(studyOwner);
        studyRepository.save(study);
        Member member = memberRepository.save(MemberFixture.createGithubMember());
        Applicant applicant = ApplicantFixture.create(study, member);
        study.addApplicant(applicant);

        // expected
        mockMvc.perform(patch("/api/study/{studyId}/approve/{applicantId}", study.getId(), member.getId())
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isCreated())
                .andDo(document("study-approve",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        pathParameters(
                                parameterWithName("studyId").description("????????? ????????? type(Long)"),
                                parameterWithName("applicantId").description("????????? ?????????")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ????????? ?????? 1?????????")
    void getStudies() throws Exception {
        // given
        Member studyOwner = memberRepository.save(MemberFixture.create(true));
        List<Study> studies = StudyFixture.createStudies(studyOwner);
        studyRepository.saveAll(studies);

        // expected
        mockMvc.perform(get("/api/study")
                        .param("page", "1")
                        .param("size", "8")
                )
                .andExpect(status().isOk())
                .andDo(document("study-get-studies",
                        requestParameters(
                                parameterWithName("page").description("?????????"),
                                parameterWithName("size").description("????????? ??? ????????? ???")
                        ),
                        responseFields(
                                fieldWithPath("totalPage").description("??? ????????? ???"),
                                fieldWithPath("studyResponses").description("????????? ????????????"),
                                fieldWithPath("studyResponses[].studyId").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].title").description("????????? ??????"),
                                fieldWithPath("studyResponses[].studyType").description("????????? ??????"),
                                fieldWithPath("studyResponses[].thumbnail").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].studyStatus").description("????????? ??????"),
                                fieldWithPath("studyResponses[].currentMemberCount").description("????????????"),
                                fieldWithPath("studyResponses[].maxMemberCount").description("??????"),
                                fieldWithPath("studyResponses[].startDate").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].endDate").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].tags").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].tags[]").description("????????? ?????? ??????"),
                                fieldWithPath("studyResponses[].createdAt").description("????????? ?????????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? ????????? ????????? ??????")
    void getMyApplies() throws Exception {
        // given
        Member applicantMember = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(applicantMember.getId(), applicantMember.getRole());

        Member studyOwner = memberRepository.save(MemberFixture.createGithubMember());
        List<Study> studies = IntStream.range(0, 10)
                .mapToObj(i -> {
                    Study study = Study.builder()
                                    .title("???????????????. " + i)
                                    .description("???????????????. " + i)
                                    .studyStatus(StudyStatus.PREPARING)
                                    .studyType(StudyType.STUDY)
                                    .startDate(LocalDate.now().plusDays(1))
                                    .endDate(LocalDate.now().plusDays(2))
                                    .owner(studyOwner)
                                    .currentMemberCount(1)
                                    .applicants(new HashSet<>())
                                    .maxMemberCount(30)
                                    .thumbnail("#00000")
                                    .tags(List.of(new Tag("??????1"), new Tag("??????2")))
                                    .build();
                    Applicant applicant = ApplicantFixture.create(study, applicantMember);
                    applicantRepository.save(applicant);
                    study.addApplicant(applicant);
                    return study;
                })
                .collect(Collectors.toList());
        studyRepository.saveAll(studies);

        // expected
        mockMvc.perform(get("/api/study/my/applies")
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andDo(document("study-get-myApplies",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        responseFields(
                                fieldWithPath("totalPage").description("??? ????????? ???"),
                                fieldWithPath("studyResponses").description("????????? ????????????"),
                                fieldWithPath("studyResponses[].studyId").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].title").description("????????? ??????"),
                                fieldWithPath("studyResponses[].studyType").description("????????? ??????"),
                                fieldWithPath("studyResponses[].thumbnail").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].studyStatus").description("????????? ??????"),
                                fieldWithPath("studyResponses[].currentMemberCount").description("????????????"),
                                fieldWithPath("studyResponses[].maxMemberCount").description("??????"),
                                fieldWithPath("studyResponses[].startDate").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].endDate").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].tags").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].tags[]").description("????????? ?????? ??????"),
                                fieldWithPath("studyResponses[].createdAt").description("????????? ?????????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? ????????? ??????")
    void getMyParticipates() throws Exception {
        // given
        Member applicantMember = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(applicantMember.getId(), applicantMember.getRole());

        Member studyOwner = memberRepository.save(MemberFixture.createGithubMember());
        List<Study> studies = IntStream.range(0, 10)
                .mapToObj(i -> {
                    Study study = Study.builder()
                                    .title("???????????????. " + i)
                                    .description("???????????????. " + i)
                                    .studyStatus(StudyStatus.PREPARING)
                                    .studyType(StudyType.STUDY)
                                    .startDate(LocalDate.now().plusDays(1))
                                    .endDate(LocalDate.now().plusDays(2))
                                    .owner(studyOwner)
                                    .currentMemberCount(1)
                                    .participants(new HashSet<>())
                                    .maxMemberCount(30)
                                    .thumbnail("#00000")
                                    .tags(List.of(new Tag("??????1"), new Tag("??????2")))
                                    .build();
                    Participant participant = ParticipantFixture.create(applicantMember, study);
                    participantRepository.save(participant);
                    study.initParticipants(participant);
                    return study;
                })
                .collect(Collectors.toList());
        studyRepository.saveAll(studies);

        // expected
        mockMvc.perform(get("/api/study/my/participates")
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andDo(document("study-get-myParticipates",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        responseFields(
                                fieldWithPath("totalPage").description("??? ????????? ???"),
                                fieldWithPath("studyResponses").description("????????? ????????????"),
                                fieldWithPath("studyResponses[].studyId").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].title").description("????????? ??????"),
                                fieldWithPath("studyResponses[].studyType").description("????????? ??????"),
                                fieldWithPath("studyResponses[].thumbnail").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].studyStatus").description("????????? ??????"),
                                fieldWithPath("studyResponses[].currentMemberCount").description("????????????"),
                                fieldWithPath("studyResponses[].maxMemberCount").description("??????"),
                                fieldWithPath("studyResponses[].startDate").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].endDate").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].tags").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].tags[]").description("????????? ?????? ??????"),
                                fieldWithPath("studyResponses[].createdAt").description("????????? ?????????")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ????????????")
    void edit() throws Exception {
        // given
        Member owner = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(owner.getId(), owner.getRole());

        Study study = studyRepository.save(StudyFixture.create(owner));

        StudyRequest request = StudyRequest.builder()
                .title("???????????????. ??????")
                .description("???????????????. ??????")
                .studyType("MOGAKKO")
                .thumbnail("#000001")
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(3))
                .maxMemberCount(2)
                .tags(List.of("??????1 ??????", "??????2 ??????"))
                .build();

        // expected
        mockMvc.perform(put("/api/study/{studyId}", study.getId())
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andDo(document("study-edit",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        pathParameters(
                                parameterWithName("studyId").description("????????? ????????? type(Long)")
                        ),
                        requestFields(
                                fieldWithPath("title").description("??????"),
                                fieldWithPath("description").description("??????"),
                                fieldWithPath("studyType").description("????????? ?????? STUDY or MOGAKKO"),
                                fieldWithPath("thumbnail").description("????????? ????????????"),
                                fieldWithPath("startDate").description("????????? ????????? ?????? (yy-MM-dd)"),
                                fieldWithPath("endDate").description("????????? ????????? ?????? (yy-MM-dd)"),
                                fieldWithPath("maxMemberCount").description("????????? ????????????"),
                                fieldWithPath("tags").description("?????? type(List)")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ????????????")
    void delete() throws Exception {
        // given
        Member owner = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(owner.getId(), owner.getRole());
        Study study = studyRepository.save(StudyFixture.create(owner));

        // expected
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/study/{studyId}", study.getId())
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isNoContent())
                .andDo(document("study-delete",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        pathParameters(
                                parameterWithName("studyId").description("????????? ????????? type(Long)")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ????????????")
    void withdraw() throws Exception {
        // given
        Member owner = memberRepository.save(MemberFixture.createGithubMember());
        Study study = studyRepository.save(StudyFixture.create(owner));

        Member participantMember = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(participantMember.getId(), participantMember.getRole());

        Participant participant = ParticipantFixture.create(participantMember, study);
        participantRepository.save(participant);
        study.initParticipants(participant);

        // expected
        mockMvc.perform(patch("/api/study/{studyId}/withdraw", study.getId())
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isCreated())
                .andDo(document("study-withdraw",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        pathParameters(
                                parameterWithName("studyId").description("????????? ????????? type(Long)")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ????????????")
    void kick() throws Exception {
        // given
        Member owner = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(owner.getId(), owner.getRole());
        Study study = studyRepository.save(StudyFixture.create(owner));

        Member member = memberRepository.save(MemberFixture.createGithubMember());
        Participant participant = ParticipantFixture.create(member, study);
        participantRepository.save(participant);
        study.initParticipants(participant);

        // expected
        mockMvc.perform(patch("/api/study/{studyId}/kick/{memberId}", study.getId(), member.getId())
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isCreated())
                .andDo(document("study-kick",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        pathParameters(
                                parameterWithName("studyId").description("????????? ????????? type(Long)"),
                                parameterWithName("memberId").description("????????? memberId")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ?????? ????????????")
    void cancelApply() throws Exception {
        // given
        Member applicantMember = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(applicantMember.getId(), applicantMember.getRole());

        Member owner = memberRepository.save(MemberFixture.createGithubMember());
        Study study = studyRepository.save(StudyFixture.create(owner));
        Applicant applicant = ApplicantFixture.create(study, applicantMember);
        study.addApplicant(applicant);
        applicantRepository.save(applicant);

        // expected
        mockMvc.perform(patch("/api/study/{studyId}/apply-cancel", study.getId())
                        .header(AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isCreated())
                .andDo(document("study-apply-cancel",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        pathParameters(
                                parameterWithName("studyId").description("????????? ????????? type(Long)")
                        )
                ));

    }

    @Test
    @DisplayName("????????? Search ????????? ?????? 1?????????")
    void getSearchStudies() throws Exception {
        // given
        Member studyOwner = memberRepository.save(MemberFixture.create(true));
        List<Study> studies = StudyFixture.createStudies(studyOwner);
        studyRepository.saveAll(studies);

        // expected
        mockMvc.perform(get("/api/study")
                        .param("page", "1")
                        .param("size", "8")
                        .param("title", "??????")
                        .param("studyType", "STUDY")
                        .param("tag", "??????1")
                )
                .andExpect(status().isOk())
                .andDo(document("study-get-search-studies",
                        requestParameters(
                                parameterWithName("page").description("?????????"),
                                parameterWithName("size").description("????????? ??? ????????? ???"),
                                parameterWithName("title").description("??????"),
                                parameterWithName("studyType").description("STUDY or MOGAKKO"),
                                parameterWithName("tag").description("??????")
                        ),
                        responseFields(
                                fieldWithPath("totalPage").description("??? ????????? ???"),
                                fieldWithPath("studyResponses").description("????????? ????????????"),
                                fieldWithPath("studyResponses[].studyId").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].title").description("????????? ??????"),
                                fieldWithPath("studyResponses[].studyType").description("????????? ??????"),
                                fieldWithPath("studyResponses[].thumbnail").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].studyStatus").description("????????? ??????"),
                                fieldWithPath("studyResponses[].currentMemberCount").description("????????????"),
                                fieldWithPath("studyResponses[].maxMemberCount").description("??????"),
                                fieldWithPath("studyResponses[].startDate").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].endDate").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].tags").description("????????? ?????????"),
                                fieldWithPath("studyResponses[].tags[]").description("????????? ?????? ??????"),
                                fieldWithPath("studyResponses[].createdAt").description("????????? ?????????")
                        )
                ));
    }
}
