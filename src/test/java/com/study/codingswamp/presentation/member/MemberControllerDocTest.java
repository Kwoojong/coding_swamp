package com.study.codingswamp.presentation.member;

import com.study.codingswamp.application.auth.token.TokenProvider;
import com.study.codingswamp.domain.member.entity.Member;
import com.study.codingswamp.domain.member.repository.MemberRepository;
import com.study.codingswamp.util.fixture.entity.member.MemberFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
@Transactional
public class MemberControllerDocTest {
    private MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TokenProvider tokenProvider;
    private final MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image".getBytes());

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint(), removeHeaders("Vary"))
                )
                .build();

        jdbcTemplate.update("alter table member auto_increment= ?", 1);
    }

    @Test
    @DisplayName("???????????? ????????? ??????????????? ??????????????? ??????.")
    void signup() throws Exception {
        // given
        String email = "seediu95@gmail.com";
        String password = "1q2w3e4r!";
        String username = "hong";

        // expected
        mockMvc.perform(multipart("/api/member")
                        .file(imageFile)
                        .param("email", email)
                        .param("password", password)
                        .param("username", username)
                ).andExpect(status().isCreated())
                .andDo(document("member-signup",
                        requestParts(
                                partWithName("imageFile").description("?????? ?????????")
                        ),
                        requestParameters(
                                parameterWithName("email").description("?????????"),
                                parameterWithName("password").description("????????????"),
                                parameterWithName("username").description("???????????????")
                        ),
                        responseFields(
                                fieldWithPath("memberId").description("?????? ????????????"),
                                fieldWithPath("email").description("?????? ?????????"),
                                fieldWithPath("githubId").description("?????? ???????????????"),
                                fieldWithPath("username").description("username or github username"),
                                fieldWithPath("imageUrl").description("?????? ?????????"),
                                fieldWithPath("profileUrl").description("?????? url"),
                                fieldWithPath("role").description("??????"),
                                fieldWithPath("joinedAt").description("?????????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ??????????????? ??????????????? ??????.")
    void getMember() throws Exception {
        Member member = memberRepository.save(MemberFixture.create(true));

        mockMvc.perform(get("/api/member/{memberId}", member.getId()))
                .andExpect(status().isOk())
                .andDo(document("member-get",
                        pathParameters(
                                parameterWithName("memberId").description("??????????????????")
                        ),
                        responseFields(
                                fieldWithPath("memberId").description("?????? ????????????"),
                                fieldWithPath("email").description("?????? ?????????"),
                                fieldWithPath("githubId").description("?????? ???????????????"),
                                fieldWithPath("username").description("username or github username"),
                                fieldWithPath("imageUrl").description("?????? ?????????"),
                                fieldWithPath("profileUrl").description("?????? url"),
                                fieldWithPath("role").description("??????"),
                                fieldWithPath("joinedAt").description("?????????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? ????????? ??????????????? ??????.")
    void edit() throws Exception {
        Member member = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(member.getId(), member.getRole());

        mockMvc.perform(multipart("/api/member/edit")
                        .file(imageFile)
                        .header(AUTHORIZATION, "Bearer " + token)
                        .param("username", "kim")
                        .param("profileUrl", "http://profile")
                )
                .andExpect(status().isCreated())
                .andDo(document("member-edit",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        ),
                        requestParts(
                                partWithName("imageFile").description("????????? ??????")
                        ),
                        requestParameters(
                                parameterWithName("username").description("???????????????"),
                                parameterWithName("profileUrl").description("????????? ????????? Url")
                        ),
                        responseFields(
                                fieldWithPath("memberId").description("?????? ????????????"),
                                fieldWithPath("email").description("?????? ?????????"),
                                fieldWithPath("githubId").description("?????? ???????????????"),
                                fieldWithPath("username").description("username or github username"),
                                fieldWithPath("imageUrl").description("?????? ?????????"),
                                fieldWithPath("profileUrl").description("?????? url"),
                                fieldWithPath("role").description("??????"),
                                fieldWithPath("joinedAt").description("?????????")
                        )
                ));
    }
    
    @Test
    @DisplayName("?????? ????????? ??????????????? ??????.")
    void delete() throws Exception {
        Member member = memberRepository.save(MemberFixture.create(true));
        String token = tokenProvider.createAccessToken(member.getId(), member.getRole());

        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/member")
                        .header(AUTHORIZATION, "Bearer " + token)
                ).andExpect(status().isNoContent())
                .andDo(document("member-delete",
                        requestHeaders(
                                headerWithName(AUTHORIZATION).description("Bearer auth credentials")
                        )
                ));
    }
}
