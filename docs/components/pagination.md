# Pagination 分页

当数据量过多时，使用分页分解数据。

## 基本用法

基础的分页用法。

![基本用法](../screenshots/pagination-default.png)

```java
import org.swelement.ui.Pagination;

// 创建分页组件（总数据量100，每页10条，初始第1页）
Pagination pagination = new Pagination(100, 10, 1);

// 监听页码变化
pagination.addPageChangeListener(page -> {
    System.out.println("当前页: " + page);
});
```

## 自定义每页数量

修改每页显示的数据量。

![自定义每页数量](../screenshots/pagination-pagesize.png)

```java
// 创建分页组件（总数据量200，每页20条）
Pagination pagination = new Pagination(200, 20, 1);

// 动态修改每页数量
pagination.setPageSize(50);
```

## Pagination 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| totalCount | 总数据量 | int | — | 0 |
| pageSize | 每页数量 | int | — | 10 |
| initialPage | 初始页码 | int | — | 1 |

## Pagination 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getTotalCount | 获取总数据量 | — | int |
| setTotal | 设置总数据量 | int t | void |
| getPageSize | 获取每页数量 | — | int |
| setPageSize | 设置每页数量 | int s | void |
| getCurrentPage | 获取当前页码 | — | int |
| setCurrentPage | 设置当前页码 | int v | void |
| getTotalPages | 获取总页数 | — | int |
| addPageChangeListener | 添加页码变化监听器 | IntConsumer l | void |
