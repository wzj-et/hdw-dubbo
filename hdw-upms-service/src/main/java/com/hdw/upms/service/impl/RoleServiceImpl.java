package com.hdw.upms.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.hdw.common.result.PageInfo;
import com.hdw.common.result.Select2Node;
import com.hdw.common.util.ArrayUtil;
import com.hdw.upms.entity.Role;
import com.hdw.upms.entity.RoleResource;
import com.hdw.upms.entity.UserRole;
import com.hdw.upms.mapper.RoleMapper;
import com.hdw.upms.mapper.RoleResourceMapper;
import com.hdw.upms.mapper.UserRoleMapper;
import com.hdw.upms.service.IRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 *
 * Role 表数据服务层接口实现类
 *
 */
@Service(
		application = "${dubbo.application.id}",
		protocol = "${dubbo.protocol.id}",
		registry = "${dubbo.registry.id}",
		group = "hdw-upms")
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

	@Autowired
	private RoleMapper roleMapper;
	@Autowired
	private UserRoleMapper userRoleMapper;
	@Autowired
	private RoleResourceMapper roleResourceMapper;

	public List<Role> selectAll() {
		EntityWrapper<Role> wrapper = new EntityWrapper<Role>();
		wrapper.orderBy("seq");
		return roleMapper.selectList(wrapper);
	}

	@Override
	public PageInfo selectDataGrid(PageInfo pageInfo) {
		Page<Map<String, Object>> page = new Page<Map<String, Object>>(pageInfo.getNowpage(), pageInfo.getSize());
		String orderField = com.baomidou.mybatisplus.toolkit.StringUtils.camelToUnderline(pageInfo.getSort());
		page.setOrderByField(orderField);
		page.setAsc(pageInfo.getOrder().equalsIgnoreCase("asc"));
		List<Map<String, Object>> list = roleMapper.selectRolePage(page, pageInfo.getCondition());
		pageInfo.setRows(list);
		pageInfo.setTotal(page.getTotal());
		return pageInfo;
	}

	@Override
	public List<Select2Node> selectTree() {
		List<Select2Node> trees = new ArrayList<Select2Node>();
		List<Role> roles = this.selectAll();
		for (Role role : roles) {
			Select2Node tree = new Select2Node();
			tree.setId(role.getId());
			tree.setText(role.getName());
			trees.add(tree);
		}
		return trees;
	}

	@Override
	public void updateRoleResource(Long roleId, String resourceIds) {
		// 先删除后添加,有点爆力
		RoleResource roleResource = new RoleResource();
		roleResource.setRoleId(roleId);
		roleResourceMapper.delete(new EntityWrapper<RoleResource>(roleResource));

		// 如果资源id为空，判断为清空角色资源
		if (StringUtils.isBlank(resourceIds)) {
			return;
		}

		String[] resourceIdArray = resourceIds.split(",");
		for (String resourceId : resourceIdArray) {
			roleResource = new RoleResource();
			roleResource.setRoleId(roleId);
			roleResource.setResourceId(Long.parseLong(resourceId));
			roleResourceMapper.insert(roleResource);
		}
	}

	@Override
	public List<Long> selectResourceIdListByRoleId(Long id) {
		return roleMapper.selectResourceIdListByRoleId(id);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public Map<String, Set<String>> selectResourceMapByUserId(Long userId) {
		Map<String, Set<String>> resourceMap = new HashMap<String, Set<String>>();
		List<Long> roleIdList = userRoleMapper.selectRoleIdListByUserId(userId);
		Set<String> urlSet = new HashSet<String>();
		Set<String> roles = new HashSet<String>();
		for (Long roleId : roleIdList) {
			List<Map<Long, String>> resourceList = roleMapper.selectResourceListByRoleId(roleId);
			if (resourceList != null) {
				for (Map<Long, String> map : resourceList) {
					if (StringUtils.isNotBlank(map.get("url"))) {
						urlSet.add(map.get("url"));
					}
				}
			}
			Role role = roleMapper.selectById(roleId);
			if (role != null) {
				roles.add(role.getName());
			}
		}
		resourceMap.put("urls", urlSet);
		resourceMap.put("roles", roles);
		return resourceMap;
	}

	@Override
	public List<Select2Node> selectTreeByUserId(Long userId) {
		List<Select2Node> tree = new ArrayList<>();
		List<UserRole> list = userRoleMapper.selectByUserId(userId);
		if (list != null && !list.isEmpty()) {
			String[] roleIds = new String[list.size()];
			for (int i = 0; i < list.size(); i++) {
				roleIds[i] = list.get(i).getRoleId().toString();
			}

			for (Select2Node s2n : this.selectTree()) {
				if (ArrayUtil.isExistenceUseList(roleIds, s2n.getId().toString())) {
					s2n.setSelected(true);
					tree.add(s2n);
				} else {
					tree.add(s2n);
				}
			}
		} else {
			tree = this.selectTree();
		}
		return tree;
	}

}