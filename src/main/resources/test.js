var data1 = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
var data2 = [150, 230, 224, 218, 135, 147, 260]

option = {
    xAxis: {
        type: 'category',
        data: data1,
    },
    yAxis: {
        type: 'value'
    },
    series: [
        {
            data: data2,
            type: 'line'
        }
    ]
};