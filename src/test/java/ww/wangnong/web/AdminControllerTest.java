package ww.wangnong.web;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.exception.BizException;
import ww.wangnong.service.AdminService;
import ww.wangnong.service.AdminService.UserInfo;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AdminService adminService;

        @Test
        void createNode_returnsOk() throws Exception {
                mockMvc.perform(post("/v1/admin/nodes")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"nodeId":"N01","nodeName":"温室1号"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        void deleteNode_returnsOk() throws Exception {
                mockMvc.perform(delete("/v1/admin/nodes/N01")
                                .header("Authorization", "Bearer admin-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        void listUsers_returnsUserList() throws Exception {
                when(adminService.listUsers("admin-token"))
                                .thenReturn(List.of(new UserInfo(1L, "用户1", "13800000001", "user")));

                mockMvc.perform(get("/v1/admin/users")
                                .header("Authorization", "Bearer admin-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data[0].userName").value("用户1"));
        }

        @Test
        void updateUserRole_returnsOk() throws Exception {
                mockMvc.perform(put("/v1/admin/users/1/role")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"role":"admin"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        void getUserNodes_returnsNodeIdList() throws Exception {
                when(adminService.getUserNodeIds("admin-token", 1L))
                                .thenReturn(List.of("N01", "N02"));

                mockMvc.perform(get("/v1/admin/users/1/nodes")
                                .header("Authorization", "Bearer admin-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data[0]").value("N01"));
        }

        @Test
        void createNode_duplicateId_returns409() throws Exception {
                doThrow(new BizException(409, "节点已存在"))
                                .when(adminService).createNode(anyString(), anyString(), anyString());

                mockMvc.perform(post("/v1/admin/nodes")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"nodeId":"N01","nodeName":"温室1号"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(409))
                                .andExpect(jsonPath("$.message").value("节点已存在"));
        }

        @Test
        void deleteNode_notFound_returns404() throws Exception {
                doThrow(new BizException(404, "节点不存在"))
                                .when(adminService).deleteNode(anyString(), anyString());

                mockMvc.perform(delete("/v1/admin/nodes/N99")
                                .header("Authorization", "Bearer admin-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(404))
                                .andExpect(jsonPath("$.message").value("节点不存在"));
        }

        @Test
        void updateUserRole_forbidden_returns403() throws Exception {
                doThrow(new BizException(403, "无权操作"))
                                .when(adminService).updateUserRole(anyString(), anyLong(), anyString());

                mockMvc.perform(put("/v1/admin/users/1/role")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"role":"admin"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(403))
                                .andExpect(jsonPath("$.message").value("无权操作"));
        }
}
