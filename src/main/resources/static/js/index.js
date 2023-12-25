$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");

	// 发送AJAX请求之前,将CSRF令牌设置到请求的消息头中.
   // var token = $("meta[name='_csrf']").attr("content");
   // var header = $("meta[name='_csrf_header']").attr("content");
   // $(document).ajaxSend(function(e, xhr, options){
   //     xhr.setRequestHeader(header, token);
   // });

	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求(POST)
	// 使用jQuery的post方法发送一个异步POST请求
	$.post(
		CONTEXT_PATH + "/discuss/add",  // 请求的URL，由基础路径和端点组成
		{"title":title, "content":content},  // 发送到服务器的数据，这里是帖子的标题和内容
		function(data) {  // 请求成功后的回调函数，参数data是从服务器返回的数据
			data = $.parseJSON(data);  // 解析从服务器返回的JSON字符串为JavaScript对象
			// 在页面的提示框中显示服务器返回的消息
			$("#hintBody").text(data.msg);
			// 显示模态提示框
			$("#hintModal").modal("show");
			// 设置一个定时器，2秒后自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 如果服务器返回的状态码是0（通常表示操作成功），则刷新页面
				if(data.code == 0) {
					window.location.reload();
				}
			}, 2000);  // 2000毫秒后执行定时器内的函数
		}
	);
}