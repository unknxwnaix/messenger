data class Friend(
    val uid: String,
    val username: String,
    val status: String, // статус дружбы (pending - запрос отправлен, accepted - запрос принят)
    val surname: String,
    val name: String,
    val avatar: String

)
{
    constructor() : this("", "", "", "", "", "")
}