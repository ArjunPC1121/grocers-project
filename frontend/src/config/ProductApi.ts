import axios from "axios";

const productApi = axios.create({
    baseURL: "http://localhost:8091/grocers/api",
});

export default productApi;