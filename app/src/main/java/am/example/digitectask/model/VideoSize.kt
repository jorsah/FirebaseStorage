package am.example.digitectask.model

data class VideoSize(val width: Int, val height: Int){
    fun isCompressable(): Boolean {
        return width > 1280 || height > 720
    }
}
