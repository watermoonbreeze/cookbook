package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.model.FoodCategory

// [AI生成] 自定义分类管理相关弹框与分类项
// 由 IngredientPickerScreen.kt 拆分而来（阶段1界面重构），保持同包同行为，不改逻辑。

/**
 * 食材分类管理弹框。[AI生成]
 *
 * 方案 A 只允许用户自建的普通分类被编辑/删除，预设分类在这里作为只读参考显示。
 */
@Composable
internal fun CategoryManageDialog(
    categories: List<FoodCategory>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (FoodCategory) -> Unit,
    onDelete: (FoodCategory) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理分类") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val editable = category.isEditableUserGeneralCategory()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (editable) 0.72f else 0.34f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${if (category.parentId == null) "" else "  "}${category.icon.ifBlank { "□" }} ${category.name}",
                            modifier = Modifier.weight(1f),
                            color = if (editable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (editable) {
                            IconButton(onClick = { onEdit(category) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Outlined.Edit, contentDescription = "编辑分类")
                            }
                            IconButton(onClick = { onDelete(category) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Outlined.Delete, contentDescription = "删除分类")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("新增分类")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}


/**
 * 新增/编辑食材分类弹框。[AI生成]
 */
@Composable
internal fun CategoryEditDialog(
    editingCategory: FoodCategory?,
    categories: List<FoodCategory>,
    name: String,
    parentId: Long?,
    onNameChange: (String) -> Unit,
    onParentChange: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val customCategories = categories.filter { it.isEditableUserGeneralCategory() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingCategory == null) "新增分类" else "编辑分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("分类名称") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                if (editingCategory == null) {
                    CategoryParentDropdown(
                        categories = customCategories,
                        selectedParentId = parentId,
                        onSelect = onParentChange,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}


/**
 * 新增分类时选择父级分类。[AI生成]
 */
@Composable
internal fun CategoryParentDropdown(
    categories: List<FoodCategory>,
    selectedParentId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedParentId }?.name
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName ?: "一级分类", modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("一级分类") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayWithParentHint()) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}


/**
 * 左侧分类项。[AI修改]
 */
@Composable
internal fun CategoryItem(
    label: String,
    level: Int,
    expanded: Boolean,
    hasChildren: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // [AI修改] 用户要求:与菜品菜系栏(CuisineRail)选中样式统一——选中项底=页面 background(在柔和左栏底上凸显)、文字主色。
    val bg = if (selected) MaterialTheme.colorScheme.background else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = (12 + (level - 1).coerceAtMost(4) * 12).dp, vertical = 12.dp), // [AI修改] 多级分类按层级缩进，避免深层过度挤压。
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (level == 1) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (hasChildren) {
            Text(if (expanded) "▾" else "▸", color = fg)
        }
    }
}

