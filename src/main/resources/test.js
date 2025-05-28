const option = {
    tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'shadow'
        },
        formatter: '{b}: {c}件'
    },
    grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
    },
    xAxis: {
        type: 'value',
        axisLabel: {
            formatter: (value) => value + '件'
        }
    },
    yAxis: {
        type: 'category',
        data: this.topProducts.map(item => item.name).reverse(),
        axisLabel: {
            formatter: (value) => {
                if (value.length > 10) {
                    return value.substring(0, 10) + '...'
                }
                return value
            }
        }
    },
    series: [{
        name: '销售数量',
        type: 'bar',
        data: this.topProducts.map(item => item.salesCount).reverse(),
        itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' }
            ])
        },
        emphasis: {
            itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: '#2378f7' },
                    { offset: 0.7, color: '#2378f7' },
                    { offset: 1, color: '#83bff6' }
                ])
            }
        }
    }]
}